package com.sriundee.preorder.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.sriundee.preorder.dto.ProductImageDraft;
import com.sriundee.preorder.dto.ProductImageDraft.PriceDraft;
import com.sriundee.preorder.dto.ProductImageDraft.VersionDraft;

/** Local rules preserve explicit shop sections and keep absent sources separate. */
@Component
public class ProductImageTextParser {
    private static final Pattern VERSION = Pattern.compile("(?iu)^([\\p{L}\\p{N}\\[(][\\p{L}\\p{M}\\p{N} &_.+/\\[\\]()-]{0,239}?)\\s+(?:ver\\.?|version|เวอร์ชัน|เวอร์ชั่น)\\s*[.:]?$");
    private static final Pattern NUMBER = Pattern.compile("(?<![\\d,a-zA-Z])-?\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?(?![\\d,.a-zA-Z])");
    private static final Pattern DEPOSIT = Pattern.compile("(?iu)มั?ดจ(?:ำ|ํา|า)|deposit");
    private static final Pattern PRICE_LABEL = Pattern.compile("(?iu)กล่อง|ราคา|บาท|price|thb");
    private static final Pattern SET_OPTION = Pattern.compile("(?iu)^((?:(?:DA[YXR]|NIG?HT)\\s+)?(?:[S$]\\s*[E\\[=]\\s*[T7]|=\\s*7(?=\\s*\\(\\s*\\d+\\s*\\))))\\b\\s*(?:\\(\\s*\\d+\\s*\\))?\\s*[:：]?");
    private static final Pattern QUANTITY_OPTION = Pattern.compile("(?iu)^(\\d{1,3})\\s*(?:(?:อัล)?บั?้?ม|albums?)\\s*(?:ราคา|price)\\s*[:：]?\\s*");
    private static final Pattern SINGLE_OPTION = Pattern.compile("(?iu)^(?:(?:อัล)?บั?้?ม\\s*ละ|เดี่ยว|สุ่ม|single\\b|random\\b)\\s*[:：]?");
    private static final Pattern EXPLICIT_NAME = Pattern.compile("(?iu)^(?:ชื่อสินค้า|สินค้า|product name)\\s*[:：]\\s*(.{1,255})$");
    private static final Pattern COVER_CHOICES = Pattern.compile("(?iu)^(ระบุปก|สุ่มปก)\\s*[:：]?\\s+(.+?)\\s*$");
    // Bare uppercase option names are common in copied sale announcements.
    // Require a trailing amount so dates, gift quantities and prose stay out.
    private static final Pattern NAMED_PRICE = Pattern.compile("^([A-Z][A-Z0-9 +&/()._-]{1,100}?)\\s+(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)(?:\\s*(?:บาท|THB))?$");
    private static final Pattern NO_POB = Pattern.compile("(?iu)(?:ไม่มี|ไม่\\s*มี|no|without)\\s*p\\s*o\\s*b");
    private static final Pattern NO_WEBSITE = Pattern.compile("(?iu)^(?:ไม่มีเว็บ|ไม่ระบุเว็บ|no\\s+(?:website|web|shop))\\s*[.:]?$" );
    private static final Pattern NO_BENEFIT_SECTION = Pattern.compile("(?ium)^(?:ไม่มี|ไม่\\s*มี|no|without)\\s*(?:การ์ด\\s*)?(?:เบเน(?:ฟิต|ฟิท)?|benefits?|pob)\\s*[.:]?$" );
    private static final Map<String, Pattern> SITE_ALIASES = new LinkedHashMap<>();
    static {
        SITE_ALIASES.put("Ktown4u", Pattern.compile("(?i)k\\s*[- ]?town\\s*4\\s*u"));
        SITE_ALIASES.put("YES24", Pattern.compile("(?i)ye[s$]\\s*24"));
        SITE_ALIASES.put("FNC STORE", Pattern.compile("(?i)fnc\\s*[s$]tore"));
        SITE_ALIASES.put("Aladin", Pattern.compile("(?i)aladin"));
        SITE_ALIASES.put("Makestar", Pattern.compile("(?i)make\\s*star"));
        SITE_ALIASES.put("Plus Chat", Pattern.compile("(?i)plus\\s*chat"));
        SITE_ALIASES.put("Mnet Plus Merch", Pattern.compile("(?i)mnet\\s*plus\\s*merch"));
        SITE_ALIASES.put("Weverse Shop", Pattern.compile("(?i)weverse\\s*shop"));
        SITE_ALIASES.put("Applemusic", Pattern.compile("(?i)apple\\s*music"));
        SITE_ALIASES.put("Soundwave", Pattern.compile("(?i)sound\\s*wave"));
        SITE_ALIASES.put("Withmuu", Pattern.compile("(?i)with\\s*muu"));
    }

    public ProductImageDraft parse(String text) {
        String raw = text == null ? "" : text.strip();
        if (raw.length() > 30_000) throw new IllegalArgumentException("ข้อความยาวเกิน 30,000 ตัวอักษร");
        Map<String, List<PriceDraft>> versions = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        Set<String> pendingSites = new LinkedHashSet<>();
        String versionName = null;
        String productName = null;
        String lastKind = "";
        boolean usedDefaultVersion = false;
        String pendingCover = null;
        boolean pendingUnnamed=false;
        Set<PriceDraft> unnamed=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        List<String> optionSites = List.of();
        List<String> chosenCovers = List.of();
        boolean randomCover = false;
        List<String> shopSectionSites = List.of();
        String readable = raw.replaceAll("\\[([^\\]\\r\\n]*)\\]\\(https?://abs\\.twimg\\.com/emoji/[^)]+\\)", "$1")
                .replaceAll("[\\x{1F90D}\\x{1F90E}\\x{1F5A4}\\x{1F499}-\\x{1F49C}\\x{2764}]\\x{FE0F}?", "\n");
        readable = leadingHeaderFromUnambiguousTrailingShop(readable);
        boolean repeatedShopSections = repeatedShopVersionSections(readable);
        List<String> sharedHeader=sharedHeaderWebsites(readable,true);
        for (String row : readable.split("\\R")) {
            String line = cleanLine(row);
            if (line.isBlank() || line.equals("ะ")) continue;
            if (NO_BENEFIT_SECTION.matcher(line).matches()) {
                shopSectionSites = List.of(WebsiteQuickAddService.NO_WEBSITE);
                pendingSites.clear(); pendingSites.add(WebsiteQuickAddService.NO_WEBSITE);
                optionSites = List.of(); chosenCovers = List.of(); pendingCover = null; pendingUnnamed = false; lastKind = "sites";
                warnings.add("ส่วน ‘ไม่มีการ์ดเบเน’ ไม่ได้ระบุเว็บใหม่ จึงแยกเป็น ‘ไม่มีเว็บ’ กรุณาตรวจแหล่งสั่งซื้อก่อนบันทึก");
                continue;
            }
            if(NO_WEBSITE.matcher(line).matches()) {
                shopSectionSites=List.of(WebsiteQuickAddService.NO_WEBSITE);pendingSites.clear();pendingSites.add(WebsiteQuickAddService.NO_WEBSITE);lastKind="sites";pendingCover=null;chosenCovers=List.of();continue;
            }
            Matcher title = EXPLICIT_NAME.matcher(line);
            if (title.matches()) { productName = title.group(1).strip(); continue; }
            Matcher choices = COVER_CHOICES.matcher(line);
            if (choices.matches()) {
                String names = choices.group(2).replaceFirst("(?iu)\\s+(?:ver\\.?|version|เวอร์ชัน|เวอร์ชั่น)\\s*[.:]?$", "");
                chosenCovers = java.util.Arrays.stream(names.split("\\s*/\\s*")).map(String::strip)
                        .filter(value -> !value.isBlank()).distinct().toList();
                if (chosenCovers.size() > 30 || chosenCovers.stream().anyMatch(value -> value.length() > 200)) {
                    warnings.add("ชื่อปกมีจำนวนหรือความยาวเกินกำหนด กรุณาตรวจแก้ข้อความ");chosenCovers=List.of();
                }
                randomCover = choices.group(1).equals("สุ่มปก");
                pendingCover = null; pendingUnnamed = false;
                lastKind = "cover";
                continue;
            }
            Matcher heading = VERSION.matcher(line);
            if (heading.matches()) {
                versionName = heading.group(1).strip().toUpperCase(Locale.ROOT) + " VER.";
                if (repeatedShopSections && lastKind.equals("sites") && !pendingSites.isEmpty()) shopSectionSites = List.copyOf(pendingSites);
                if (!lastKind.equals("sites")) pendingSites.clear();
                lastKind = "version";
                pendingCover = null;
                optionSites = List.of();
                chosenCovers = List.of();
                continue;
            }
            Matcher setOption = SET_OPTION.matcher(line);
            Matcher quantityOption = QUANTITY_OPTION.matcher(line);
            Matcher singleOption = SINGLE_OPTION.matcher(line);
            boolean quantityBundle = quantityOption.find() && Integer.parseInt(quantityOption.group(1)) >= 2;
            boolean bundle = setOption.find() || quantityBundle;
            boolean single = !bundle && singleOption.find();
            boolean explicitSingle=single && singleOption.group().strip().matches("(?iu)^(?:เดี่ยว|สุ่ม|single|random).*" );
            String coverName = quantityBundle ? "SET(" + Integer.parseInt(quantityOption.group(1)) + ")" : bundle ? setOption.group(1).toUpperCase(Locale.ROOT).startsWith("DA") ? "DAY SET"
                    : setOption.group(1).toUpperCase(Locale.ROOT).startsWith("NI") ? "NIGHT SET" : "SET" : single ? singleOption.group().strip().matches("(?iu)^(?:สุ่ม|random).*" )?"สุ่ม":"เดี่ยว" : pendingCover;
            if (bundle && !quantityBundle) {
                Matcher quantity = Pattern.compile("\\(\\s*(\\d+)\\s*\\)").matcher(setOption.group());
                if (quantity.find()) coverName += "(" + quantity.group(1) + ")";
            }
            // Strip the SET quantity before reading money: SET(3) 1,750 is 1,750, not 3.
            String priceLine = quantityBundle ? line.substring(quantityOption.end()).strip() : bundle ? line.substring(setOption.end()).strip() : single ? line.substring(singleOption.end()).strip() : line;
            Matcher namedPrice = NAMED_PRICE.matcher(line);
            boolean named = !bundle && !single && namedPrice.matches()
                    && !namedPrice.group(1).matches(".*(?:GIFT|CARD|POB|COPYRIGHT|DATE|YEAR|PERIOD|BUY|GET).*" )
                    && new BigDecimal(namedPrice.group(2).replace(",", "")).compareTo(BigDecimal.TEN) >= 0;
            if (named) { coverName = namedPrice.group(1).strip(); priceLine = namedPrice.group(2); }
            if ((bundle || single) && priceLine.isBlank()) {
                pendingCover = coverName;pendingUnnamed=single&&!explicitSingle; lastKind = "cover"; continue;
            }
            Matcher depositLabel = DEPOSIT.matcher(line.replace(" ", ""));
            boolean hasDeposit = depositLabel.find();
            boolean optionPrice = (bundle || single || pendingCover != null) && priceLine.matches("^-?\\d[\\d,]*(?:\\.\\d{1,2})?(?:\\s.*)?$");
            if (named || PRICE_LABEL.matcher(priceLine).find() || hasDeposit || optionPrice) {
                // A gift quantity (e.g. 1 box, 7 cards) is not a price.
                List<BigDecimal> amounts = numbers(priceLine);
                boolean explicitPrice = named || optionPrice || priceLine.matches("(?iu).*(?:กล่อง\\s*ละ|ราคา|price|thb|\\d\\s*บาท).*" );
                if (!amounts.isEmpty() && (explicitPrice || hasDeposit)) {
                    BigDecimal price = amounts.getFirst();
                    BigDecimal deposit = null;
                    String compact = priceLine.replace(" ", "");
                    Matcher marker = DEPOSIT.matcher(compact);
                    if (marker.find()) {
                        String pricePrefix = compact.substring(0, marker.start()).replaceAll("[\\s(:：]+$", "")
                                .replaceFirst("(?iu)(?:บาท|THB)$", "");
                        List<BigDecimal> deposits = numbers(compact.substring(marker.end()));
                        if (!deposits.isEmpty()) deposit = deposits.getFirst();
                        // A deposit-only line must not become a selling price.
                        if (numbers(pricePrefix).isEmpty() || !pricePrefix.matches(".*\\d$")) {
                            warnings.add("พบยอดมัดจำแยกบรรทัด กรุณาตรวจและรวมกับบรรทัดราคาในข้อความที่อ่านได้");
                            continue;
                        }
                        // Money follows a quantity/label, immediately before the deposit clause.
                        // A damaged SET label must never turn its quantity into the selling price.
                        price = numbers(pricePrefix).getLast();
                    }
                    if (price.signum() <= 0 || price.compareTo(new BigDecimal("999999999.99")) > 0) continue;
                    if (deposit == null && !hasDeposit) deposit = BigDecimal.ZERO;
                    // Cropping may intentionally exclude the product/version heading. Keep the usable
                    // price groups and use the form's standard single-version name for review.
                    String groupVersion = versionName == null ? "Standard" : versionName;
                    if (versionName == null) usedDefaultVersion = true;
                    if (deposit != null && deposit.compareTo(price) > 0) {
                        warnings.add(groupVersion + ": OCR อ่านราคา " + price.toPlainString() + " แต่มัดจำ " + deposit.toPlainString()
                                + " สูงกว่าราคา จึงเว้นราคาไว้ให้ตรวจแก้ทั้งสองยอดก่อนบันทึก");
                        price = null;
                    } else if (deposit != null && deposit.signum() < 0) {
                        warnings.add(groupVersion + ": ยอดมัดจำที่อ่านได้ไม่สัมพันธ์กับราคา จึงเว้นไว้ให้ตรวจเติม");
                        deposit = null;
                    }
                    List<String> groupSites = !pendingSites.isEmpty() ? new ArrayList<>(pendingSites)
                            : coverName != null && (lastKind.equals("price") || lastKind.equals("cover")) && !optionSites.isEmpty() ? optionSites
                            : !shopSectionSites.isEmpty() ? shopSectionSites
                            : List.of(WebsiteQuickAddService.NO_WEBSITE);
                    if (groupSites.equals(List.of(WebsiteQuickAddService.NO_WEBSITE))) warnings.add(groupVersion + ": ไม่ได้ระบุเว็บ เลือก ‘ไม่มีเว็บ’ ให้แล้ว กรุณาตรวจทาน");
                    if (deposit == null) warnings.add(groupVersion + ": ไม่พบยอดมัดจำที่ชัดเจน กรุณาตรวจเติม");
                    List<String> offerNames = !bundle && !named && !chosenCovers.isEmpty()
                            ? randomCover ? List.of("สุ่ม (" + String.join(" / ", chosenCovers) + ")") : chosenCovers
                            : List.of(coverName == null ? "ทั่วไป" : coverName);
                    for (String offerName : offerNames) {
                        PriceDraft offer=new PriceDraft(offerName, groupSites, price, deposit);
                        versions.computeIfAbsent(groupVersion, key -> new ArrayList<>()).add(offer);
                        if(chosenCovers.isEmpty() && (coverName==null || single&&!explicitSingle || !named&&!bundle&&!single&&pendingCover!=null&&pendingUnnamed))unnamed.add(offer);
                    }
                    chosenCovers = List.of();
                    optionSites = groupSites;
                    pendingCover = null;
                    pendingSites.clear();
                    lastKind = "price";
                    continue;
                }
            }
            List<String> sites = sites(line);
            if (!sites.isEmpty()) {
                String header = line.replaceAll("^[^\\p{L}\\p{N}]+", "").strip();
                boolean shopHeader = sites.size() == 1 && header.matches("(?iu).*(?:shop|store)\\s*$")
                        && header.replaceAll("[^\\p{L}\\p{N}]", "").equalsIgnoreCase(sites.getFirst().replaceAll("[^\\p{L}\\p{N}]", ""));
                shopSectionSites = shopHeader ? List.copyOf(sites) : !sharedHeader.isEmpty()?sharedHeader:List.of();
                if (!lastKind.equals("sites")) pendingSites.clear();
                pendingSites.addAll(sites);
                if (NO_POB.matcher(line).find()) {
                    warnings.add(String.join(", ", sites) + ": ระบุว่าไม่มี POB กรุณาตรวจเงื่อนไขของแถมก่อนบันทึก");
                }
                lastKind = "sites";
                pendingCover = null;
                chosenCovers = List.of();
            } else {
                // Unrelated text breaks the connection to a heading, rather than guessing its meaning.
                pendingSites.clear();
                lastKind = "other";
                pendingCover = null;
                optionSites = List.of();
                chosenCovers = List.of();
            }
        }
        if (usedDefaultVersion) warnings.add("ภาพส่วนนี้ไม่ได้ระบุเวอร์ชัน จัดกลุ่มไว้ใน ‘Standard’ ให้แล้ว สามารถแก้ชื่อเวอร์ชันในฟอร์มได้");
        if (versions.isEmpty()) warnings.add("ยังไม่พบราคาที่จัดกลุ่มได้ กรุณาตรวจข้อความราคาและชื่อเว็บที่อ่านได้ หรือกรอกข้อมูลเอง");
        warnings.add("ตรวจชื่อสินค้า ศิลปิน ประเภท วันที่ และของแถมจากภาพอีกครั้ง ระบบไม่เดาข้อมูลที่ไม่ได้ระบุ");
        return new ProductImageDraft(productName, versions.entrySet().stream()
                .map(entry -> new VersionDraft(entry.getKey(), ProductImageCoverNaming.apply(entry.getValue(),unnamed))).toList(),
                warnings.stream().distinct().toList(), raw);
    }

    private List<BigDecimal> numbers(String text) {
        List<BigDecimal> result = new ArrayList<>();
        Matcher matcher = NUMBER.matcher(text);
        while (matcher.find()) result.add(new BigDecimal(matcher.group().replace(",", "")));
        return result;
    }

    private static String cleanLine(String row) {
        return row.strip().replace('−', '-').replaceAll("[\\t ]+", " ")
                .replaceAll("^[\\p{So}\\p{Sk}\\x{FE0F}\\x{200D}•●▪]+\\s*", "");
    }

    /** Matching multi-version price lists under separate shop headers form independent shop sections.
     * A later unmatched version (such as a standalone DESK offer) must not inherit an earlier shop. */
    private static boolean repeatedShopVersionSections(String text) {
        List<List<String>> sections = new ArrayList<>();
        List<String> current = null;
        boolean previousSites = false;
        for (String row : text.split("\\R")) {
            String line = cleanLine(row); if (line.isBlank() || line.equals("ะ")) continue;
            Matcher version = VERSION.matcher(line);
            if (version.matches()) { if (current != null) current.add(version.group(1).toUpperCase(Locale.ROOT)); previousSites = false; continue; }
            if (COVER_CHOICES.matcher(line).matches() || PRICE_LABEL.matcher(line).find() || DEPOSIT.matcher(line).find()) { previousSites = false; continue; }
            if (!sites(line).isEmpty()) {
                if (!previousSites) { current = new ArrayList<>(); sections.add(current); }
                previousSites = true;
            } else previousSites = false;
        }
        if (sections.size() < 2 || sections.getFirst().size() < 2) return false;
        List<String> first = sections.getFirst();
        return sections.stream().allMatch(section -> section.equals(first));
    }

    /** Only a single terminal shop after an otherwise unassigned price block can move above it.
     * Existing shop headers, explicit no-shop text and unrelated prose keep their original scope. */
    private static String leadingHeaderFromUnambiguousTrailingShop(String text) {
        List<String> lines = text.lines().map(ProductImageTextParser::cleanLine).filter(line -> !line.isBlank()).toList();
        if (lines.size() < 2) return text;
        String last = lines.getLast();
        List<String> names = sites(last);
        if (names.size() != 1 || !WebsiteQuickAddService.normalizedName(last).equals(WebsiteQuickAddService.normalizedName(names.getFirst()))) return text;
        boolean hasPrice = false;
        for (String line : lines.subList(0, lines.size() - 1)) {
            if (VERSION.matcher(line).matches() || COVER_CHOICES.matcher(line).matches() || EXPLICIT_NAME.matcher(line).matches()) continue;
            if (NO_WEBSITE.matcher(line).matches() || !sites(line).isEmpty()) return text;
            if (PRICE_LABEL.matcher(line).find() || DEPOSIT.matcher(line).find()
                    || SINGLE_OPTION.matcher(line).find() || SET_OPTION.matcher(line).find() || NAMED_PRICE.matcher(line).matches()) hasPrice = true;
            else return text;
        }
        return hasPrice ? last + "\n" + String.join("\n", lines.subList(0, lines.size() - 1)) : text;
    }

    /** Repair AI hierarchy only when every source offer is covered by explicit cover directives
     * and its shop/amount pairs agree with the validated AI result. Never replace differing prices. */
    ProductImageDraft reconcileExplicitCoverChoices(ProductImageDraft draft) {
        Set<String> labels = new LinkedHashSet<>();
        for (String row : draft.rawText().split("\\R")) {
            Matcher choices = COVER_CHOICES.matcher(row.strip().replaceAll("^[\\p{So}\\p{Sk}\\x{FE0F}•●▪]+\\s*", ""));
            if (!choices.matches()) continue;
            String names = choices.group(2).replaceFirst("(?iu)\\s+(?:ver\\.?|version|เวอร์ชัน|เวอร์ชั่น)\\s*[.:]?$", "");
            List<String> parts = java.util.Arrays.stream(names.split("\\s*/\\s*")).map(String::strip).distinct().toList();
            if (choices.group(1).equals("สุ่มปก")) labels.add("สุ่ม (" + String.join(" / ", parts) + ")");
            else labels.addAll(parts);
        }
        if (labels.isEmpty()) return draft;
        ProductImageDraft parsed = parse(draft.rawText());
        List<PriceDraft> source = parsed.versions().stream().flatMap(version -> version.groups().stream()).toList();
        List<PriceDraft> actual = draft.versions().stream().flatMap(version -> version.groups().stream()).toList();
        if (source.isEmpty() || source.stream().anyMatch(offer -> !labels.contains(offer.name())
                && !offer.name().matches("(?iu)(?:(?:DAY|NIGHT) )?SET(?:\\(\\d+\\))?"))) return draft;
        Set<OfferAmounts> expected = source.stream().map(this::offerAmounts).collect(java.util.stream.Collectors.toSet());
        Set<OfferAmounts> received = actual.stream().map(this::offerAmounts).collect(java.util.stream.Collectors.toSet());
        if (!expected.equals(received)) return draft;
        List<String> warnings = new ArrayList<>(draft.warnings());
        warnings.addAll(parsed.warnings());
        warnings.add("แยกปกตามเงื่อนไขระบุปก / สุ่มปก โดยตรวจราคาและเว็บตรงกับผล AI แล้ว กรุณาตรวจทานก่อนบันทึก");
        return new ProductImageDraft(draft.productName(), parsed.versions(), warnings.stream().distinct().toList(), draft.rawText());
    }
    private record OfferAmounts(List<String> websites, BigDecimal price, BigDecimal deposit) {}
    private OfferAmounts offerAmounts(PriceDraft offer) {
        return new OfferAmounts(offer.websites().stream().map(WebsiteQuickAddService::normalizedName).sorted().toList(),
                offer.price() == null ? null : offer.price().stripTrailingZeros(),
                offer.deposit() == null ? null : offer.deposit().stripTrailingZeros());
    }

    /** A single multi-shop heading immediately preceding the first version scopes the whole price block.
     * Multiple shop blocks, prices before any version, or explicit no-shop text remain local. */
    public static List<String> commonHeaderWebsites(String raw) {
        return sharedHeaderWebsites(raw,false);
    }
    static boolean hasNoBenefitSection(String raw) { return raw != null && NO_BENEFIT_SECTION.matcher(raw).find(); }
    private static List<String> sharedHeaderWebsites(String raw,boolean allowExplicitNoWeb) {
        if(raw==null || !allowExplicitNoWeb && (hasNoBenefitSection(raw) || raw.contains("ไม่มีเว็บ") || raw.contains("ไม่ระบุเว็บ") || Pattern.compile("(?iu)no\\s+(?:website|web|shop)").matcher(raw).find()))return List.of();
        int blocks=0;boolean previousSites=false,seenVersion=false,headerFollowedByVersion=false,priceBeforeVersion=false;
        List<String> header=new ArrayList<>();
        for(String row:raw.split("\\R")) {
            String line=row.strip();if(line.isBlank()||line.equals("ะ"))continue;
            if(VERSION.matcher(line).matches()){if(!seenVersion)headerFollowedByVersion=previousSites;seenVersion=true;previousSites=false;continue;}
            if(DEPOSIT.matcher(line.replace(" ","")).find()||PRICE_LABEL.matcher(line).find()) {if(!seenVersion)priceBeforeVersion=true;previousSites=false;continue;}
            List<String> names=sites(line);
            if(!names.isEmpty()){if(!previousSites)blocks++;if(!seenVersion)for(String name:names)if(!header.contains(name))header.add(name);previousSites=true;}
            else previousSites=false;
        }
        return blocks==1 && header.size()>1 && headerFollowedByVersion && !priceBeforeVersion?List.copyOf(header):List.of();
    }
    private static List<String> sites(String line) {
        List<String> found = new ArrayList<>();
        SITE_ALIASES.forEach((name, pattern) -> { if (pattern.matcher(line).find()) found.add(name); });
        // A standalone shop/store name is useful even before it has been added to master data.
        String standalone = cleanLine(line);
        if (found.isEmpty() && standalone.matches("(?i)^[a-z][a-z0-9 .&'_-]{0,69}(?:shop|store)$")) found.add(standalone);
        // Preserve unfamiliar names in a slash-separated header so the user can map them manually.
        if (line.contains("/") && !line.matches(".*[ก-๙].*") && !line.matches(".*\\d{3,}.*")
                && !line.contains(":") && line.length() <= 250) {
            for (String token : line.split("/")) {
                String name = token.replaceAll("^[^\\p{L}\\d]+|[^\\p{L}\\d]+$", "").strip();
                if (name.isBlank()) continue;
                boolean known = SITE_ALIASES.values().stream().anyMatch(pattern -> pattern.matcher(name).find());
                if (!known && name.length() <= 80) found.add(name);
            }
        }
        return found;
    }
}
