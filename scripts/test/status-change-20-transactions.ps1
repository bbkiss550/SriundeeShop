param(
    [string]$AppBaseUrl = "http://localhost:8080",
    [string]$Database = "db_sriundee_shop",
    [string]$MySqlUser = "sriundee_shop",
    [string]$MySqlPassword = "sriundee_shop",
    [string]$MySqlPath = ""
)

$ErrorActionPreference = "Stop"
$TransactionCount = 20

function Find-MySqlPath {
    param([string]$Candidate)

    if ($Candidate -and (Test-Path -LiteralPath $Candidate)) {
        return (Resolve-Path -LiteralPath $Candidate).Path
    }

    $command = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $knownPaths = @(
        "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
        "C:\Program Files\MySQL\MySQL Server 9.0\bin\mysql.exe",
        "C:\xampp\mysql\bin\mysql.exe"
    )
    foreach ($path in $knownPaths) {
        if (Test-Path -LiteralPath $path) {
            return $path
        }
    }

    throw "mysql.exe was not found. Pass -MySqlPath with the full mysql.exe path."
}

function Invoke-MySql {
    param([Parameter(Mandatory = $true)][string]$Sql)

    $oldPassword = $env:MYSQL_PWD
    $env:MYSQL_PWD = $MySqlPassword
    try {
        $result = & $script:ResolvedMySqlPath "-u$MySqlUser" "--database=$Database" "--batch" "--raw" "--skip-column-names" "-e" $Sql
        if ($LASTEXITCODE -ne 0) {
            throw "MySQL command failed."
        }
        return @($result)
    } finally {
        $env:MYSQL_PWD = $oldPassword
    }
}

function Get-Scalar {
    param([Parameter(Mandatory = $true)][string]$Sql)

    $result = @(Invoke-MySql -Sql $Sql)
    if (-not $result -or $null -eq $result[0]) {
        return ""
    }
    return $result[0].ToString().Trim()
}

function Invoke-JsonPost {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)]$Payload
    )

    return Invoke-RestMethod -Method Post -Uri ($AppBaseUrl.TrimEnd("/") + $Path) `
        -ContentType "application/json" -Body ($Payload | ConvertTo-Json -Depth 8 -Compress)
}

function Invoke-StatusChange {
    param(
        [Parameter(Mandatory = $true)][int[]]$Ids,
        [Parameter(Mandatory = $true)][int]$NewStatus,
        [Parameter(Mandatory = $true)][string]$RecordDate,
        [hashtable]$Extra = @{}
    )

    $payload = @{
        ids = @($Ids)
        orderStatus = $NewStatus
        recordDate = $RecordDate
    }
    foreach ($key in $Extra.Keys) {
        $payload[$key] = $Extra[$key]
    }
    Invoke-JsonPost -Path "/change/status/update" -Payload $payload | Out-Null
}

function Assert-Count {
    param(
        [Parameter(Mandatory = $true)][string]$Label,
        [Parameter(Mandatory = $true)][int]$Expected,
        [Parameter(Mandatory = $true)][string]$Sql
    )

    $actual = [int](Get-Scalar -Sql $Sql)
    if ($actual -ne $Expected) {
        throw "$Label expected $Expected but got $actual."
    }
    Write-Host ("OK  {0}: {1}" -f $Label, $actual)
}

function Get-CostAmount {
    param([Parameter(Mandatory = $true)][int[]]$Ids, [decimal]$Multiplier)

    $idList = $Ids -join ","
    $total = [decimal](Get-Scalar -Sql "SELECT COALESCE(SUM(od_price_total), 0) FROM t_order_detail WHERE ID_order_detail IN ($idList);")
    return [math]::Round([double]($total * $Multiplier), 2)
}

$script:ResolvedMySqlPath = Find-MySqlPath -Candidate $MySqlPath
$RunTag = "STATUS20-" + (Get-Date -Format "yyyyMMdd-HHmmss")
$EscapedRunTag = $RunTag.Replace("'", "''")
$RunStart = [datetime]"2026-07-01"

Write-Host "Checking app and database..."
$changePage = Invoke-WebRequest -UseBasicParsing -Uri ($AppBaseUrl.TrimEnd("/") + "/change")
if ($changePage.StatusCode -ne 200) {
    throw "The application did not return 200 for /change."
}
Invoke-MySql -Sql "SELECT 1;" | Out-Null

Write-Host "Creating tagged STATUS20 master data for $RunTag..."
Invoke-MySql -Sql @"
INSERT INTO t_artist (a_name, ID_group, a_logo, a_delete)
VALUES ('$EscapedRunTag ARTIST', 5, '', 'A');
INSERT INTO t_type (t_name, t_delete)
VALUES ('$EscapedRunTag TYPE', 'A');
INSERT INTO t_website (w_name, w_delete)
VALUES ('$EscapedRunTag WEB', 'A');
SET @status20_artist = (SELECT ID_art FROM t_artist WHERE a_name = '$EscapedRunTag ARTIST' ORDER BY ID_art DESC LIMIT 1);
SET @status20_type = (SELECT ID_type FROM t_type WHERE t_name = '$EscapedRunTag TYPE' ORDER BY ID_type DESC LIMIT 1);
SET @status20_web = (SELECT ID_web FROM t_website WHERE w_name = '$EscapedRunTag WEB' ORDER BY ID_web DESC LIMIT 1);
INSERT INTO t_product (p_name, ID_type, ID_art, p_end_date, p_send_date, ID_pro_status, p_delete, p_pic)
VALUES ('$EscapedRunTag PRODUCT', @status20_type, @status20_artist, '2026-07-31', '2026-08-15', 1, 'A', '');
SET @status20_product = (SELECT ID_product FROM t_product WHERE p_name = '$EscapedRunTag PRODUCT' ORDER BY ID_product DESC LIMIT 1);
INSERT INTO t_product_web (ID_pro, ID_web)
VALUES (@status20_product, @status20_web);
INSERT INTO t_version (ID_pro, v_name, v_delete)
VALUES (@status20_product, '$EscapedRunTag VERSION', 'A');
SET @status20_version = (SELECT ID_ver FROM t_version WHERE ID_pro = @status20_product ORDER BY ID_ver DESC LIMIT 1);
INSERT INTO t_cover (ID_pro, ID_web, ID_ver, c_name, c_price_total, c_price_pledge, c_price_balance, c_delete)
VALUES (@status20_product, @status20_web, @status20_version, '$EscapedRunTag COVER', '990', '350', '640', 'A');
DELETE FROM t_order_detail WHERE ID_order IS NULL;
"@ | Out-Null

$coverId = [int](Get-Scalar -Sql "SELECT ID_cover FROM t_cover WHERE c_name = '$EscapedRunTag COVER' ORDER BY ID_cover DESC LIMIT 1;")
if (-not $coverId) {
    throw "STATUS20 cover seed failed."
}

Write-Host "Saving $TransactionCount full-payment orders..."
$detailIds = New-Object System.Collections.Generic.List[int]
for ($i = 1; $i -le $TransactionCount; $i++) {
    $qty = (($i - 1) % 3) + 1
    $productTotal = [decimal](990 * $qty)
    $productPledge = [decimal](350 * $qty)
    $productBalance = [decimal](640 * $qty)
    $sendCost = [decimal]50
    $net = $productTotal + $sendCost
    $orderDate = $RunStart.AddDays(($i - 1) % 20).ToString("yyyy-MM-dd")

    Invoke-JsonPost -Path "/order/detail/save" -Payload @{
        cover = $coverId
        qty = $qty
        price_total = $productTotal
        price_pledge = $productPledge
        price_balance = $productBalance
    } | Out-Null

    $savedOrder = Invoke-JsonPost -Path "/order/save" -Payload @{
        customer_name = ("{0}-CUS-{1:000}" -f $RunTag, $i)
        pay_method = 1
        pay_type = $null
        order_date = $orderDate
        last_pay_date = ""
        send_cost = $sendCost
        discount = 0
        price_total = $productTotal
        price_pledge = 0
        price_balance = 0
        net = $net
        remark = "$RunTag status change $i"
    }
    $orderId = [int]$savedOrder.orderId
    $detailId = [int](Get-Scalar -Sql "SELECT ID_order_detail FROM t_order_detail WHERE ID_order = $orderId ORDER BY ID_order_detail DESC LIMIT 1;")
    $detailIds.Add($detailId)
}

$status2Ids = @($detailIds)
$status3Ids = @($detailIds | Select-Object -Skip 5)
$status4Ids = @($detailIds | Select-Object -Skip 10)
$status5Ids = @($detailIds | Select-Object -Skip 15)

Write-Host "Changing STATUS20 details through the status flow..."
Invoke-StatusChange -Ids $status2Ids -NewStatus 2 -RecordDate "2026-07-21" -Extra @{
    costPrice = (Get-CostAmount -Ids $status2Ids -Multiplier 0.55)
    costNote = "$RunTag press cost"
}
Invoke-StatusChange -Ids $status3Ids -NewStatus 3 -RecordDate "2026-07-22" -Extra @{
    l_lot_number = "$RunTag-LOT-01"
    l_start_date = "2026-07-25"
    l_end_date = "2026-07-31"
}
Invoke-StatusChange -Ids $status4Ids -NewStatus 4 -RecordDate "2026-08-01" -Extra @{
    shippingPrice = (Get-CostAmount -Ids $status4Ids -Multiplier 0.07)
    shippingNote = "$RunTag shipping cost"
    l_arrive_date = "2026-08-01"
}
Invoke-StatusChange -Ids $status5Ids -NewStatus 5 -RecordDate "2026-08-03" -Extra @{
    postalPrice = (Get-CostAmount -Ids $status5Ids -Multiplier 0.03)
    postalNote = "$RunTag postal cost"
}

Write-Host ""
Write-Host "Verifying STATUS20 batch..."
Assert-Count -Label "tagged STATUS20 orders" -Expected 20 -Sql "SELECT COUNT(*) FROM t_order WHERE o_remark LIKE '$EscapedRunTag%';"
Assert-Count -Label "tagged STATUS20 details" -Expected 20 -Sql "SELECT COUNT(*) FROM t_order_detail od JOIN t_order o ON o.ID_order = od.ID_order WHERE o.o_remark LIKE '$EscapedRunTag%';"
foreach ($status in 2..5) {
    Assert-Count -Label "tagged STATUS20 status $status" -Expected 5 -Sql "SELECT COUNT(*) FROM t_order_detail od JOIN t_order o ON o.ID_order = od.ID_order WHERE o.o_remark LIKE '$EscapedRunTag%' AND od.ID_order_status = $status;"
}

Write-Host ""
Write-Host "Current live status totals after STATUS20 insert:"
Invoke-MySql -Sql @"
SELECT CONCAT('status_', ID_order_status, '=', COUNT(*))
FROM t_order_detail
GROUP BY ID_order_status
ORDER BY ID_order_status;
"@ | ForEach-Object { Write-Host $_ }
Write-Host "Run tag: $RunTag"
Write-Host "Cleanup: apply scripts/test/truncate-status-change-test-data.sql when STATUS20 data is no longer needed."
