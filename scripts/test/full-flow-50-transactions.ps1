param(
    [string]$AppBaseUrl = "http://localhost:8080",
    [string]$Database = "db_sriundee_shop",
    [string]$MySqlUser = "sriundee_shop",
    [string]$MySqlPassword = "sriundee_shop",
    [string]$MySqlPath = "",
    [int]$TransactionCount = 50
)

$ErrorActionPreference = "Stop"

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

if ($TransactionCount -ne 50) {
    throw "This production-like scenario is fixed at 50 transactions so its assertions stay meaningful."
}

$script:ResolvedMySqlPath = Find-MySqlPath -Candidate $MySqlPath
$RunTag = "FLOW50-" + (Get-Date -Format "yyyyMMdd-HHmmss")
$EscapedRunTag = $RunTag.Replace("'", "''")
$RunStart = [datetime]"2026-05-01"

Write-Host "Checking app and database..."
$orderPage = Invoke-WebRequest -UseBasicParsing -Uri ($AppBaseUrl.TrimEnd("/") + "/order")
if ($orderPage.StatusCode -ne 200) {
    throw "The application did not return 200 for /order."
}
Invoke-MySql -Sql "SELECT 1;" | Out-Null

Write-Host "Creating tagged test master data for $RunTag..."
$masterSeed = @"
INSERT INTO t_artist (a_name, ID_group, a_logo, a_delete)
VALUES ('$EscapedRunTag ARTIST-A', 5, '', 'A'),
       ('$EscapedRunTag ARTIST-B', 5, '', 'A'),
       ('$EscapedRunTag ARTIST-C', 5, '', 'A');
INSERT INTO t_type (t_name, t_delete)
VALUES ('$EscapedRunTag ALBUM', 'A'),
       ('$EscapedRunTag GOODS', 'A'),
       ('$EscapedRunTag LIGHT', 'A');
INSERT INTO t_website (w_name, w_delete)
VALUES ('$EscapedRunTag WEB-A', 'A'),
       ('$EscapedRunTag WEB-B', 'A'),
       ('$EscapedRunTag WEB-C', 'A');
SET @flow_art_a = (SELECT ID_art FROM t_artist WHERE a_name = '$EscapedRunTag ARTIST-A' ORDER BY ID_art DESC LIMIT 1);
SET @flow_art_b = (SELECT ID_art FROM t_artist WHERE a_name = '$EscapedRunTag ARTIST-B' ORDER BY ID_art DESC LIMIT 1);
SET @flow_art_c = (SELECT ID_art FROM t_artist WHERE a_name = '$EscapedRunTag ARTIST-C' ORDER BY ID_art DESC LIMIT 1);
SET @flow_type_a = (SELECT ID_type FROM t_type WHERE t_name = '$EscapedRunTag ALBUM' ORDER BY ID_type DESC LIMIT 1);
SET @flow_type_b = (SELECT ID_type FROM t_type WHERE t_name = '$EscapedRunTag GOODS' ORDER BY ID_type DESC LIMIT 1);
SET @flow_type_c = (SELECT ID_type FROM t_type WHERE t_name = '$EscapedRunTag LIGHT' ORDER BY ID_type DESC LIMIT 1);
INSERT INTO t_product (p_name, ID_type, ID_art, p_end_date, p_send_date, ID_pro_status, p_delete, p_pic)
VALUES ('$EscapedRunTag PRODUCT-A', @flow_type_a, @flow_art_a, '2026-05-31', '2026-06-15', 1, 'A', ''),
       ('$EscapedRunTag PRODUCT-B', @flow_type_b, @flow_art_b, '2026-06-05', '2026-06-22', 1, 'A', ''),
       ('$EscapedRunTag PRODUCT-C', @flow_type_c, @flow_art_c, '2026-06-10', '2026-06-30', 1, 'A', '');
SET @flow_product_a = (SELECT ID_product FROM t_product WHERE p_name = '$EscapedRunTag PRODUCT-A' ORDER BY ID_product DESC LIMIT 1);
SET @flow_product_b = (SELECT ID_product FROM t_product WHERE p_name = '$EscapedRunTag PRODUCT-B' ORDER BY ID_product DESC LIMIT 1);
SET @flow_product_c = (SELECT ID_product FROM t_product WHERE p_name = '$EscapedRunTag PRODUCT-C' ORDER BY ID_product DESC LIMIT 1);
SET @flow_web_a = (SELECT ID_web FROM t_website WHERE w_name = '$EscapedRunTag WEB-A' ORDER BY ID_web DESC LIMIT 1);
SET @flow_web_b = (SELECT ID_web FROM t_website WHERE w_name = '$EscapedRunTag WEB-B' ORDER BY ID_web DESC LIMIT 1);
SET @flow_web_c = (SELECT ID_web FROM t_website WHERE w_name = '$EscapedRunTag WEB-C' ORDER BY ID_web DESC LIMIT 1);
INSERT INTO t_product_web (ID_pro, ID_web)
VALUES (@flow_product_a, @flow_web_a), (@flow_product_b, @flow_web_b), (@flow_product_c, @flow_web_c);
INSERT INTO t_version (ID_pro, v_name, v_delete)
VALUES (@flow_product_a, '$EscapedRunTag VER-A', 'A'),
       (@flow_product_b, '$EscapedRunTag VER-B', 'A'),
       (@flow_product_c, '$EscapedRunTag VER-C', 'A');
SET @flow_ver_a = (SELECT ID_ver FROM t_version WHERE ID_pro = @flow_product_a ORDER BY ID_ver DESC LIMIT 1);
SET @flow_ver_b = (SELECT ID_ver FROM t_version WHERE ID_pro = @flow_product_b ORDER BY ID_ver DESC LIMIT 1);
SET @flow_ver_c = (SELECT ID_ver FROM t_version WHERE ID_pro = @flow_product_c ORDER BY ID_ver DESC LIMIT 1);
INSERT INTO t_cover (ID_pro, ID_web, ID_ver, c_name, c_price_total, c_price_pledge, c_price_balance, c_delete)
VALUES (@flow_product_a, @flow_web_a, @flow_ver_a, '$EscapedRunTag COVER-A', '1290', '500', '790', 'A'),
       (@flow_product_b, @flow_web_b, @flow_ver_b, '$EscapedRunTag COVER-B', '890', '300', '590', 'A'),
       (@flow_product_c, @flow_web_c, @flow_ver_c, '$EscapedRunTag COVER-C', '1590', '650', '940', 'A');
DELETE FROM t_order_detail WHERE ID_order IS NULL;
"@
Invoke-MySql -Sql $masterSeed | Out-Null

$coverRows = Invoke-MySql -Sql @"
SELECT CONCAT(ID_cover, '|', c_price_total, '|', c_price_pledge, '|', c_price_balance)
FROM t_cover
WHERE c_name LIKE '$EscapedRunTag COVER-%'
ORDER BY ID_cover;
"@
$covers = @($coverRows | ForEach-Object {
    $parts = $_.ToString().Split("|")
    [pscustomobject]@{
        Id = [int]$parts[0]
        Total = [decimal]$parts[1]
        Pledge = [decimal]$parts[2]
        Balance = [decimal]$parts[3]
    }
})
if ($covers.Count -ne 3) {
    throw "Expected 3 tagged covers but found $($covers.Count)."
}

Write-Host "Saving 50 orders through /order/detail/save and /order/save..."
$orderIds = New-Object System.Collections.Generic.List[int]
$detailIds = New-Object System.Collections.Generic.List[int]
$depositOrderIds = New-Object System.Collections.Generic.List[int]
for ($i = 1; $i -le $TransactionCount; $i++) {
    $cover = $covers[($i - 1) % $covers.Count]
    $qty = (($i - 1) % 4) + 1
    $productTotal = [decimal]($cover.Total * $qty)
    $productPledge = [decimal]($cover.Pledge * $qty)
    $productBalance = [decimal]($cover.Balance * $qty)
    $sendCost = [decimal](50 + (($i - 1) % 3) * 10)
    $discount = if (($i % 7) -eq 0) { [decimal]20 } else { [decimal]0 }
    $net = $productTotal + $sendCost - $discount
    $payMethod = if (($i % 2) -eq 0) { 2 } else { 1 }
    $orderDate = $RunStart.AddDays(($i - 1) % 28).ToString("yyyy-MM-dd")

    Invoke-JsonPost -Path "/order/detail/save" -Payload @{
        cover = $cover.Id
        qty = $qty
        price_total = $productTotal
        price_pledge = $productPledge
        price_balance = $productBalance
    } | Out-Null

    $orderPayload = @{
        customer_name = ("{0}-CUS-{1:000}" -f $RunTag, $i)
        pay_method = $payMethod
        pay_type = $null
        order_date = $orderDate
        last_pay_date = ""
        send_cost = $sendCost
        discount = $discount
        price_total = $productTotal
        price_pledge = 0
        price_balance = 0
        net = $net
        remark = "$RunTag order $i"
    }
    if ($payMethod -eq 2) {
        $orderPayload.pay_type = if (($i % 4) -eq 0) { 2 } else { 1 }
        $orderPayload.price_pledge = $productPledge
        $orderPayload.price_balance = $net - $productPledge
        if ($orderPayload.pay_type -eq 2) {
            $orderPayload.last_pay_date = $RunStart.AddDays(40 + ($i % 8)).ToString("yyyy-MM-dd")
        }
    }

    $savedOrder = Invoke-JsonPost -Path "/order/save" -Payload $orderPayload
    $orderId = [int]$savedOrder.orderId
    $detailId = [int](Get-Scalar -Sql "SELECT ID_order_detail FROM t_order_detail WHERE ID_order = $orderId ORDER BY ID_order_detail DESC LIMIT 1;")
    $orderIds.Add($orderId)
    $detailIds.Add($detailId)
    if ($payMethod -eq 2) {
        $depositOrderIds.Add($orderId)
    }
}

Write-Host "Receiving the remaining deposit for 10 deposit orders..."
$balanceOrders = @($depositOrderIds | Select-Object -First 10)
foreach ($orderId in $balanceOrders) {
    Invoke-RestMethod -Method Post -Uri ($AppBaseUrl.TrimEnd("/") + "/deposit-balance/$orderId/receive?recordDate=2026-06-05") | Out-Null
}

$pressedIds = @($detailIds | Select-Object -Skip 10)
$lotIds = @($detailIds | Select-Object -Skip 20)
$arrivedIds = @($detailIds | Select-Object -Skip 30)
$sentIds = @($detailIds | Select-Object -Skip 40)

Write-Host "Moving order status groups through press, lot, shipping, and postal flow..."
Invoke-StatusChange -Ids $pressedIds -NewStatus 2 -RecordDate "2026-05-18" -Extra @{
    costPrice = (Get-CostAmount -Ids $pressedIds -Multiplier 0.55)
    costNote = "$RunTag press cost"
}
Invoke-StatusChange -Ids $lotIds -NewStatus 3 -RecordDate "2026-05-20" -Extra @{
    l_lot_number = "$RunTag-LOT-01"
    l_start_date = "2026-05-25"
    l_end_date = "2026-05-31"
}
Invoke-StatusChange -Ids $arrivedIds -NewStatus 4 -RecordDate "2026-06-01" -Extra @{
    shippingPrice = (Get-CostAmount -Ids $arrivedIds -Multiplier 0.07)
    shippingNote = "$RunTag shipping cost"
    l_arrive_date = "2026-06-01"
}
Invoke-StatusChange -Ids $sentIds -NewStatus 5 -RecordDate "2026-06-03" -Extra @{
    postalPrice = (Get-CostAmount -Ids $sentIds -Multiplier 0.03)
    postalNote = "$RunTag postal cost"
}

Write-Host "Saving remaining expense types through /cost/expense..."
$expenseTypes = @(3, 4, 99)
for ($index = 0; $index -lt $expenseTypes.Count; $index++) {
    Invoke-WebRequest -UseBasicParsing -Method Post -Uri ($AppBaseUrl.TrimEnd("/") + "/cost/expense") -Body @{
        recordDate = "2026-06-04"
        typeCost = $expenseTypes[$index]
        price = 150 + ($index * 75)
        note = "$RunTag extra expense type $($expenseTypes[$index])"
    } | Out-Null
}

Write-Host ""
Write-Host "Verifying stored flow data..."
Assert-Count -Label "tagged orders" -Expected 50 -Sql "SELECT COUNT(*) FROM t_order WHERE o_remark LIKE '$EscapedRunTag%';"
Assert-Count -Label "tagged order details" -Expected 50 -Sql "SELECT COUNT(*) FROM t_order_detail od JOIN t_order o ON o.ID_order = od.ID_order WHERE o.o_remark LIKE '$EscapedRunTag%';"
Assert-Count -Label "tagged income rows" -Expected 60 -Sql "SELECT COUNT(*) FROM t_income i JOIN t_order o ON o.ID_order = i.ID_order WHERE o.o_remark LIKE '$EscapedRunTag%';"
Assert-Count -Label "tagged costs" -Expected 6 -Sql "SELECT COUNT(*) FROM t_cost WHERE c_note LIKE '$EscapedRunTag%';"
Assert-Count -Label "tagged lots" -Expected 1 -Sql "SELECT COUNT(*) FROM t_lot WHERE l_lot_number LIKE '$EscapedRunTag%';"

Write-Host ""
Write-Host "Financial summary for $RunTag"
Invoke-MySql -Sql @"
SELECT CONCAT('orders=', COUNT(*),
              ', sales_net=', FORMAT(COALESCE(SUM(o_net), 0), 2),
              ', product_value=', FORMAT(COALESCE(SUM(o_price_total), 0), 2),
              ', balance_open=', FORMAT(COALESCE(SUM(CASE WHEN ID_pay_method = 2 THEN o_price_balance ELSE 0 END), 0), 2))
FROM t_order
WHERE o_remark LIKE '$EscapedRunTag%';
SELECT CONCAT('income_type_', ID_type_income, '=', FORMAT(COALESCE(SUM(CAST(REPLACE(c_price, ',', '') AS DECIMAL(14,2))), 0), 2))
FROM t_income i
JOIN t_order o ON o.ID_order = i.ID_order
WHERE o.o_remark LIKE '$EscapedRunTag%'
GROUP BY ID_type_income
ORDER BY ID_type_income;
SELECT CONCAT('cost_type_', ID_type_cost, '=', FORMAT(COALESCE(SUM(CAST(REPLACE(c_price, ',', '') AS DECIMAL(14,2))), 0), 2))
FROM t_cost
WHERE c_note LIKE '$EscapedRunTag%'
GROUP BY ID_type_cost
ORDER BY ID_type_cost;
SELECT CONCAT('status_', od.ID_order_status, '=', COUNT(*))
FROM t_order_detail od
JOIN t_order o ON o.ID_order = od.ID_order
WHERE o.o_remark LIKE '$EscapedRunTag%'
GROUP BY od.ID_order_status
ORDER BY od.ID_order_status;
"@ | ForEach-Object { Write-Host $_ }

Write-Host ""
Write-Host "Checking output pages..."
$dashboard = Invoke-WebRequest -UseBasicParsing -Uri ($AppBaseUrl.TrimEnd("/") + "/?startDate=2026-01-01&endDate=2026-12-31")
$reports = Invoke-WebRequest -UseBasicParsing -Uri ($AppBaseUrl.TrimEnd("/") + "/reports?startDate=2026-01-01&endDate=2026-12-31")
$orderList = Invoke-WebRequest -UseBasicParsing -Uri ($AppBaseUrl.TrimEnd("/") + "/orders?startDate=2026-01-01&endDate=2026-12-31")
Write-Host ("dashboard_http={0}; reports_http={1}; order_list_http={2}" -f $dashboard.StatusCode, $reports.StatusCode, $orderList.StatusCode)
Write-Host "Run tag: $RunTag"
Write-Host "Cleanup: apply scripts/test/truncate-full-flow-test-data.sql when the inserted test data is no longer needed."
