package com.sriundee.preorder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.Files;
import java.nio.file.Path;

class ProductImageTextParserTest {
    @Test
    @EnabledIfSystemProperty(named="ocr.benefit.cached",matches=".+")
    void actualNoBenefitPosterKeepsTheSectionBoundaryAndTwoAlbumSet() throws Exception {
        var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(Files.readString(Path.of(System.getProperty("ocr.benefit.cached"))));
        var lines=new java.util.ArrayList<String>();json.path("lines").forEach(row->lines.add(row.path("text").asText()));
        var draft=parser.parse(String.join("\n",lines));
        var offers=draft.versions().stream().flatMap(version->version.groups().stream()).toList();
        assertThat(draft.versions()).hasSize(4);assertThat(offers).hasSize(6);
        assertThat(offers.stream().filter(offer->offer.websites().equals(java.util.List.of("ไม่มีเว็บ"))).map(offer->offer.price().intValue()).toList()).containsExactly(550,350,250,2220);
        var tenuto=draft.versions().get(1).groups().getFirst();assertThat(tenuto.name()).isEqualTo("SET(2)");assertThat(tenuto.price()).isEqualByComparingTo("780");assertThat(tenuto.deposit()).isEqualByComparingTo("400");
    }
    @Test void noBenefitHeadingBreaksStoreScopeAndThaiTwoAlbumPriceIsASet() throws Exception {
        var draft=parser.parse("""
            🎁Everline / KPOP TOGETHER / Whosfan Store
            Pulse ver.
            บั้มละ650 (มัดจำ350)
            Tenuto ver.
            2 บั้ม ราคา780 (มัดจำ400)
            ไม่มีการ์ดเบเน
            Pulse ver.
            บั้มละ550 (มัดจำ300)
            Tenuto ver.
            บั้มละ350 (มัดจำ200)
            Digging ver.
            บั้มละ250 (มัดจำ100)
            Jam ver.
            บั้มละ2,220 (มัดจำ1,000)
            """);
        assertThat(draft.versions()).hasSize(4);
        var pulse=draft.versions().get(0).groups();var tenuto=draft.versions().get(1).groups();
        assertThat(pulse.get(0).websites()).containsExactlyInAnyOrder("Everline","KPOP TOGETHER","Whosfan Store");
        assertThat(tenuto.get(0).websites()).containsExactlyInAnyOrder("Everline","KPOP TOGETHER","Whosfan Store");
        assertThat(tenuto.get(0).name()).isEqualTo("SET(2)");assertThat(tenuto.get(0).price()).isEqualByComparingTo("780");assertThat(tenuto.get(0).deposit()).isEqualByComparingTo("400");
        assertThat(pulse.get(1).websites()).containsExactly("ไม่มีเว็บ");assertThat(pulse.get(1).price()).isEqualByComparingTo("550");
        assertThat(tenuto.get(1).websites()).containsExactly("ไม่มีเว็บ");assertThat(tenuto.get(1).price()).isEqualByComparingTo("350");
        assertThat(draft.versions().get(2).groups().getFirst().websites()).containsExactly("ไม่มีเว็บ");
        assertThat(draft.versions().get(3).groups().getFirst().websites()).containsExactly("ไม่มีเว็บ");
        assertThat(ProductImageTextParser.commonHeaderWebsites(draft.rawText())).isEmpty();
        assertThat(draft.warnings()).anyMatch(message->message.contains("ไม่มีการ์ดเบเน"));
        Path folder=Path.of(".runtime/product-setup-qa");Files.createDirectories(folder);
        Files.writeString(folder.resolve("no-benefit-draft.json"),new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(draft));
    }
    @Test void explicitStoreWithoutPobStaysExplicitAndStandaloneNoPobStartsNewScope() {
        var explicit=parser.parse("Ktown4u ไม่มี POB\nPHOTOBOOK VER.\nบั้มละ490 (มัดจำ250)");
        assertThat(explicit.versions().getFirst().groups().getFirst().websites()).containsExactly("Ktown4u");
        for(String heading:java.util.List.of("ไม่มี POB","ไม่มีเบเน","No benefit")) {
            var draft=parser.parse("Weverse Shop\nPHOTOBOOK VER.\nราคา490 (มัดจำ250)\n"+heading+"\nราคา350 (มัดจำ200)");
            assertThat(draft.versions().getFirst().groups().getLast().websites()).containsExactly("ไม่มีเว็บ");
        }
    }
    @Test void setLabelsDamagedByOcrKeepQuantityAndNeverUseItAsThePrice() {
        for(String label:java.util.List.of("s=7(2)","=7(2)","se7(2)")) {
            var offer=parser.parse("Applemusic\nStandard ver.\n"+label+" 950 (มัดจำ500)").versions().getFirst().groups().getFirst();
            assertThat(offer.name()).isEqualTo("SET(2)");assertThat(offer.price()).isEqualByComparingTo("950");assertThat(offer.deposit()).isEqualByComparingTo("500");
        }
    }
    @Test
    @EnabledIfSystemProperty(named="ocr.columns.cached",matches=".+")
    void actualTwoColumnPosterKeepsIndependentShopPrices() throws Exception {
        var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(Files.readString(Path.of(System.getProperty("ocr.columns.cached"))));
        var lines=new java.util.ArrayList<String>();json.path("lines").forEach(row->lines.add(row.path("text").asText()));
        var draft=parser.parse(String.join("\n",lines));
        var offers=draft.versions().stream().flatMap(version->version.groups().stream()).toList();
        assertThat(draft.versions()).hasSize(5);
        assertThat(offers).hasSize(14);
        assertThat(offers.stream().filter(offer->offer.websites().equals(java.util.List.of("Applemusic"))).map(offer->offer.price().intValue()).toList()).containsExactly(490,950,330,650,450,1090,1420);
        assertThat(offers.stream().filter(offer->offer.websites().contains("YES24")).map(offer->offer.price().intValue()).toList()).containsExactly(550,1050,350,690,490,1150,1520);
    }
    @Test void independentShopSectionsKeepAllFiveVersionsAndFourteenPriceGroups() throws Exception {
        var draft=parser.parse("""
            Applemusic
            Standard ver.
            บั้มละ490 (มัดจำ250)
            SET(2)950 (มัดจำ500)
            Platform Album POCAALBUM ver.
            บั้มละ330 (มัดจำ150)
            SET(2)650 (มัดจำ300)
            SAGA ver.
            บั้มละ450 (มัดจำ250)
            Light ver.
            บั้มละ1090 (มัดจำ600)
            LP ver.
            บั้มละ1420 (มัดจำ700)
            FANS SHOP / YES24 / Musicplant
            Standard ver.
            บั้มละ550 (มัดจำ250)
            SET(2)1050 (มัดจำ500)
            Platform Album POCAALBUM ver.
            บั้มละ350 (มัดจำ150)
            SET(2)690 (มัดจำ300)
            SAGA ver.
            บั้มละ490 (มัดจำ250)
            Light ver.
            บั้มละ1150 (มัดจำ600)
            LP ver.
            บั้มละ1520 (มัดจำ700)
            """);
        assertThat(draft.versions()).hasSize(5);
        var offers=draft.versions().stream().flatMap(version->version.groups().stream()).toList();
        assertThat(offers).hasSize(14);
        assertThat(offers.stream().filter(offer->offer.websites().equals(java.util.List.of("Applemusic"))).map(offer->offer.price().intValue()).toList()).containsExactly(490,950,330,650,450,1090,1420);
        var right=offers.stream().filter(offer->offer.websites().contains("YES24")).toList();
        assertThat(right).hasSize(7).allSatisfy(offer->assertThat(offer.websites()).containsExactlyInAnyOrder("FANS SHOP","YES24","Musicplant"));
        assertThat(right).extracting(offer->offer.price().intValue()).containsExactly(550,1050,350,690,490,1150,1520);
        assertThat(draft.versions().getFirst().groups()).extracting(offer->offer.name()).containsExactly("สุ่ม","SET(2)","สุ่ม","SET(2)");
        assertThat(draft.warnings()).noneMatch(message->message.contains("ไม่ได้ระบุเว็บ"));
        Path folder=Path.of(".runtime/product-setup-qa");Files.createDirectories(folder);
        Files.writeString(folder.resolve("two-column-draft.json"),new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(draft));
    }
    @Test void standaloneUnknownShopIsReadWithEmojiAboveOrBelowPrice() throws Exception {
        for(String text:java.util.List.of("🐈TBLshop\n🤍บั้มละ490 (มัดจำ250)","🤍บั้มละ490 (มัดจำ250)\n🐈TBLshop","🐈‍⬛TBLshop\n🤍บั้มละ490 (มัดจำ250)")) {
            var draft=parser.parse(text);
            var offer=draft.versions().getFirst().groups().getFirst();
            assertThat(offer.websites()).containsExactly("TBLshop");
            assertThat(offer.price()).isEqualByComparingTo("490");assertThat(offer.deposit()).isEqualByComparingTo("250");
            assertThat(draft.rawText()).isEqualTo(text);
            assertThat(draft.warnings()).noneMatch(message->message.contains("ไม่ได้ระบุเว็บ"));
        }
        Path folder=Path.of(".runtime/product-setup-qa");Files.createDirectories(folder);
        Files.writeString(folder.resolve("unknown-shop-draft.json"),new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(parser.parse("🐈TBLshop\n🤍บั้มละ490 (มัดจำ250)")));
        assertThat(parser.parse("New Music Store\nราคา490").versions().getFirst().groups().getFirst().websites()).containsExactly("New Music Store");
    }
    @Test void trailingShopCannotOverrideExistingOrExplicitlyMissingSources() {
        var explicit=parser.parse("ไม่มีเว็บ\nราคา490 (มัดจำ250)\nTBLshop");
        assertThat(explicit.versions().getFirst().groups().getFirst().websites()).containsExactly("ไม่มีเว็บ");
        var existing=parser.parse("YES24\nราคา490 (มัดจำ250)\nTBLshop");
        assertThat(existing.versions().getFirst().groups().getFirst().websites()).containsExactly("YES24");
        var unrelated=parser.parse("ราคา490 (มัดจำ250)\nรายละเอียดเพิ่มเติม\nTBLshop");
        assertThat(unrelated.versions().getFirst().groups().getFirst().websites()).containsExactly("ไม่มีเว็บ");
        var ambiguous=parser.parse("ราคา490 (มัดจำ250)\nTBLshop\nราคา600 (มัดจำ300)");
        assertThat(ambiguous.versions().getFirst().groups().getFirst().websites()).containsExactly("ไม่มีเว็บ");
        assertThat(ambiguous.versions().getFirst().groups().getLast().websites()).containsExactly("TBLshop");
    }
    @Test void bracketedVersionHeadingsStaySeparateAndSetQuantityIsPreserved() throws Exception {
        var draft=parser.parse("""
            Weverse Shop
            CHEMICAL NIGHT [STUDIO CHOOM GIFT] ver.
            บั้มละ 750 (มัดจำ350)
            COMPACT [STUDIO CHOOM GIFT] ver.
            บั้มละ 430 (มัดจำ200)
            SET(5) 2,100 (มัดจำ1,100)
            """);
        assertThat(draft.versions()).extracting(version->version.name()).containsExactly("CHEMICAL NIGHT [STUDIO CHOOM GIFT] VER.","COMPACT [STUDIO CHOOM GIFT] VER.");
        assertThat(draft.versions().getFirst().groups()).hasSize(1);
        assertThat(draft.versions().getFirst().groups().getFirst().name()).isEqualTo("เดี่ยว");
        assertThat(draft.versions().get(1).groups()).extracting(offer->offer.name()).containsExactly("สุ่ม","SET(5)");
        assertThat(draft.versions().get(1).groups().get(1).price()).isEqualByComparingTo("2100");
        assertThat(draft.versions().get(1).groups().get(1).deposit()).isEqualByComparingTo("1100");
        assertThat(draft.versions().stream().flatMap(version->version.groups().stream()).toList()).allSatisfy(offer->assertThat(offer.websites()).containsExactly("Weverse Shop"));
        Path folder=Path.of(".runtime/product-setup-qa");Files.createDirectories(folder);
        Files.writeString(folder.resolve("bracket-version-draft.json"),new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(draft));
    }
    @Test void multipleParenthesesAndSquareBracketsArePartOfVersionName() {
        var draft=parser.parse("Applemusic / Ktown4u\nALBUM (A) (B) [GIFT] VER.\nบั้มละ590 (มัดจำ300)\nCOMPACT (C) [GIFT] VER.\nบั้มละ400 (มัดจำ200)");
        assertThat(draft.versions()).extracting(version->version.name()).containsExactly("ALBUM (A) (B) [GIFT] VER.","COMPACT (C) [GIFT] VER.");
        assertThat(draft.versions().stream().flatMap(version->version.groups().stream()).toList()).allSatisfy(offer->assertThat(offer.websites()).containsExactlyInAnyOrder("Applemusic","Ktown4u"));
    }
    @Test void explicitSelectableAndRandomCoversUseStandardAndKeepShopPrices() throws Exception {
        var draft=parser.parse("""
            🎁Ktown4u
            ระบุปก Fallen Angel / Alt ver.
            บั้มละ 380 บาท (มัดจำ 150)
            SET(2) 750 บาท (มัดจำ 300)
            🎁Withmuu / Jump Up
            ระบุปก Fallen Angel / Alt ver.
            บั้มละ 450 บาท (มัดจำ 200)
            🎁Everline / Makestar
            สุ่มปก Fallen Angel / Alt ver.
            บั้มละ 450 บาท (มัดจำ 200)
            รายละเอียดเพิ่มเติมในเมนชั่น
            """);
        assertThat(draft.versions()).hasSize(1);
        assertThat(draft.versions().getFirst().name()).isEqualTo("Standard");
        var offers=draft.versions().getFirst().groups();
        assertThat(offers).extracting(offer->offer.name()).containsExactly("Fallen Angel","Alt","SET(2)","Fallen Angel","Alt","สุ่ม (Fallen Angel / Alt)");
        assertThat(offers.subList(0,3)).allSatisfy(offer->assertThat(offer.websites()).containsExactly("Ktown4u"));
        assertThat(offers.subList(3,5)).allSatisfy(offer->{assertThat(offer.websites()).containsExactlyInAnyOrder("Withmuu","Jump Up");assertThat(offer.price()).isEqualByComparingTo("450");assertThat(offer.deposit()).isEqualByComparingTo("200");});
        assertThat(offers.get(5).websites()).containsExactlyInAnyOrder("Everline","Makestar");
        assertThat(offers.get(0).price()).isEqualByComparingTo("380");assertThat(offers.get(1).price()).isEqualByComparingTo("380");
        assertThat(offers.get(2).price()).isEqualByComparingTo("750");assertThat(offers.get(2).deposit()).isEqualByComparingTo("300");
        Path folder=Path.of(".runtime/product-setup-qa");Files.createDirectories(folder);
        Files.writeString(folder.resolve("explicit-cover-draft.json"),new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(draft));
    }
    @Test void explicitCoverNamesStayUnderRealParentVersionAndDoNotLeakToNextShop() {
        var draft=parser.parse("Ktown4u\nPHOTOBOOK VER.\nระบุปก A / B ver.\nบั้มละ 380 บาท (มัดจำ 150)\nSET(2) 750 บาท (มัดจำ300)\nYES24\nบั้มละ 400 (มัดจำ200)");
        assertThat(draft.versions()).hasSize(1);
        assertThat(draft.versions().getFirst().name()).isEqualTo("PHOTOBOOK VER.");
        assertThat(draft.versions().getFirst().groups()).extracting(offer->offer.name()).containsExactly("A","B","SET(2)","เดี่ยว");
        assertThat(draft.versions().getFirst().groups().getLast().websites()).containsExactly("YES24");
    }
    @Test void copiedMembershipAnnouncementWithoutPriceLabelsHasThreeNamedOffers() {
        String text="[🤍](https://abs.twimg.com/emoji/v2/svg/1f90d.svg)MEMBERSHIP 610 [🤍](https://abs.twimg.com/emoji/v2/svg/1f90d.svg)KIT 900 [🤍](https://abs.twimg.com/emoji/v2/svg/1f90d.svg)MEMBERSHIP + KIT 1,490";
        var draft=parser.parse(text);
        assertThat(draft.rawText()).isEqualTo(text);
        assertThat(draft.versions()).hasSize(1);
        assertThat(draft.versions().getFirst().name()).isEqualTo("Standard");
        var offers=draft.versions().getFirst().groups();
        assertThat(offers).extracting(offer->offer.name()).containsExactly("MEMBERSHIP","KIT","MEMBERSHIP + KIT");
        assertThat(offers).extracting(offer->offer.price()).containsExactly(new BigDecimal("610"),new BigDecimal("900"),new BigDecimal("1490"));
        assertThat(offers).allSatisfy(offer->{assertThat(offer.deposit()).isEqualByComparingTo("0");assertThat(offer.websites()).containsExactly("ไม่มีเว็บ");});
    }
    @Test void plainNamedPricesKeepSharedShopAndDoNotReadGiftQuantitiesAsPrices() {
        var draft=parser.parse("Weverse Shop\nMEMBERSHIP 610\nKIT 900\nMEMBERSHIP + KIT 1,490");
        assertThat(draft.versions().getFirst().groups()).hasSize(3).allSatisfy(offer->assertThat(offer.websites()).containsExactly("Weverse Shop"));
        assertThat(parser.parse("PHOTOCARD 100\nYEAR 2026\nBUY 100\nGIFT 300").versions()).isEmpty();
    }
    @Test void onlyUnnamedOptionsPairedWithSetInTheirOwnShopGroupBecomeRandom() {
        var draft=parser.parse("""
            Weverse Shop
            PHOTOBOOK VER.
            บั้มละ 590 (มัดจำ300)
            SET(3) 1750 (มัดจำ900)
            EXPLICIT VER.
            เดี่ยว 400 (มัดจำ200)
            DAY SET(6) 2390 (มัดจำ1200)
            WALLET VER.
            บั้มละ 1050 (มัดจำ600)
            """);
        assertThat(draft.versions().get(0).groups().getFirst().name()).isEqualTo("สุ่ม");
        assertThat(draft.versions().get(1).groups().getFirst().name()).isEqualTo("เดี่ยว");
        assertThat(draft.versions().get(2).groups().getFirst().name()).isEqualTo("เดี่ยว");
        var separate=parser.parse("YES24\nPHOTOBOOK VER.\nบั้มละ590 (มัดจำ300)\nKtown4u\nSET(3)1750 (มัดจำ900)");
        assertThat(separate.versions().getFirst().groups().getFirst().name()).isEqualTo("เดี่ยว");
    }
    @Test void handlesUnnamedGenericPricesAndSplitLabelsAndPreservesExplicitRandom() {
        assertThat(parser.parse("PHOTOBOOK VER.\nราคา590 (มัดจำ300)\nSET 1750 (มัดจำ900)").versions().getFirst().groups().getFirst().name()).isEqualTo("สุ่ม");
        assertThat(parser.parse("PHOTOBOOK VER.\nบั้มละ\nราคา590 (มัดจำ300)\nSET(3)1750 (มัดจำ900)").versions().getFirst().groups().getFirst().name()).isEqualTo("สุ่ม");
        assertThat(parser.parse("PHOTOBOOK VER.\nสุ่ม590 (มัดจำ300)").versions().getFirst().groups().getFirst().name()).isEqualTo("สุ่ม");
    }
    @Test
    void screenshotMultiShopHeadingAppliesToAllFourVersionsAndSevenOffers() throws Exception {
        var draft=parser.parse("""
            applemusic / ktown4u / soundwave
            photobook ver.
            ะ
            บมละ 550 (มดจา 300)
            se7(3) 1,590 (มัดจำ900)
            compact ver.
            ะ
            บั้มละ 360 (มัดจำ150)
            SET(6) 2,100 (มัดจำ1,000)
            wallet ver.
            บั้มละ 1,050 (มัดจำ600)
            fancy ver.
            บั้มละ 360 (มัดจำ150)
            SET(6) 2,100 (มัดจำ1,000)
            รายละเอียดเพิ่มเติมในเมนชั่น
            """);
        assertThat(draft.versions()).extracting(v->v.name()).containsExactly("PHOTOBOOK VER.","COMPACT VER.","WALLET VER.","FANCY VER.");
        var offers=draft.versions().stream().flatMap(version->version.groups().stream()).toList();
        assertThat(offers).hasSize(7).allSatisfy(offer->assertThat(offer.websites()).containsExactlyInAnyOrder("Applemusic","Ktown4u","Soundwave"));
        assertThat(offers).extracting(offer->offer.price().intValue()).containsExactly(550,1590,360,2100,1050,360,2100);
        assertThat(offers).extracting(offer->offer.deposit().intValue()).containsExactly(300,900,150,1000,600,150,1000);
        assertThat(offers).extracting(offer->offer.name()).containsExactly("สุ่ม","SET(3)","สุ่ม","SET(6)","เดี่ยว","สุ่ม","SET(6)");
        java.nio.file.Path path=java.nio.file.Path.of(".runtime/product-setup-qa/shared-shop-draft.json");java.nio.file.Files.createDirectories(path.getParent());new com.fasterxml.jackson.databind.ObjectMapper().writeValue(path.toFile(),draft);
    }
    @Test void explicitNoWebsiteOverridesSharedHeaderWithoutChangingEarlierVersions() {
        var draft=parser.parse("Applemusic / Ktown4u / Soundwave\nPHOTOBOOK VER.\nเดี่ยว 550 (มัดจำ300)\nCOMPACT VER.\nเดี่ยว 360 (มัดจำ150)\nDESK VER.\nไม่มีเว็บ\nราคา950 (มัดจำ500)");
        assertThat(draft.versions().get(1).groups().getFirst().websites()).containsExactlyInAnyOrder("Applemusic","Ktown4u","Soundwave");
        assertThat(draft.versions().get(2).groups().getFirst().websites()).containsExactly("ไม่มีเว็บ");
    }
    @Test void unrelatedTextAfterMultiShopHeaderDoesNotCreateGlobalScope() {
        var draft=parser.parse("Applemusic / Ktown4u / Soundwave\nของแถมพิเศษ\nDESK VER.\nราคา950 (มัดจำ500)");
        assertThat(draft.versions().getFirst().groups().getFirst().websites()).containsExactly("ไม่มีเว็บ");
    }
    @Test
    @EnabledIfSystemProperty(named = "ocr.set.cached", matches = ".+")
    void allSixRowsFromActualFullPosterHaveExactShopCoverPriceAndDeposit() throws Exception {
        var draft = parser.parse(Files.readString(Path.of(System.getProperty("ocr.set.cached"))));
        assertThat(draft.versions().stream().flatMap(version -> version.groups().stream().map(group ->
                version.name() + "|" + group.name() + "|" + group.price().toPlainString() + "|" + group.deposit().toPlainString())).toList())
                .containsExactly("PHOTOBOOK VER.|สุ่ม|590|300", "PHOTOBOOK VER.|SET(3)|1750|900",
                    "ENGENE VER.|สุ่ม|400|200", "ENGENE VER.|DAY SET(6)|2390|1200", "ENGENE VER.|NIGHT SET(6)|2390|1200",
                    "WEVERSE ALBUMS VER.|เดี่ยว|300|150");
        assertThat(draft.versions().stream().flatMap(version -> version.groups().stream()).toList())
                .allSatisfy(group -> assertThat(group.websites()).containsExactly("Weverse Shop"));
    }
    @Test
    void separatesSinglesAndSetsIntoCoversWithoutTreatingSetQuantitiesAsPrices() {
        var draft = parser.parse("""
                Weverse Shop
                PHOTOBOOK ver.
                บั้มละ 590 (มัดจำ 300)
                SET(3) 1,750 (มัดจำ 900)
                ENGENE ver.
                บั้มละ 400 (มัดจำ 200)
                DAY SET(6) 2,390 (มัดจำ 1,200)
                NIGHT SET(6) 2,390 (มัดจำ 1,200)
                Weverse Albums ver.
                บั้มละ 300 (มัดจำ 150)
                """);
        assertThat(draft.versions()).hasSize(3);
        var photo = draft.versions().getFirst().groups();
        assertThat(photo).extracting(group -> group.name()).containsExactly("สุ่ม", "SET(3)");
        assertThat(photo.get(1).price()).isEqualByComparingTo("1750");
        assertThat(photo.get(1).deposit()).isEqualByComparingTo("900");
        assertThat(photo.get(1).websites()).containsExactly("Weverse Shop");
        var engene = draft.versions().get(1).groups();
        assertThat(engene).extracting(group -> group.name()).containsExactly("สุ่ม", "DAY SET(6)", "NIGHT SET(6)");
        assertThat(engene).extracting(group -> group.price()).containsExactly(new BigDecimal("400"), new BigDecimal("2390"), new BigDecimal("2390"));
        assertThat(engene.get(1).deposit()).isEqualByComparingTo("1200");
        assertThat(engene.get(2).deposit()).isEqualByComparingTo("1200");
        assertThat(draft.versions().get(2).groups().getFirst().name()).isEqualTo("เดี่ยว");
        assertThat(draft.versions().stream().flatMap(version -> version.groups().stream()).toList())
                .allSatisfy(group -> assertThat(group.websites()).containsExactly("Weverse Shop"));
    }

    @Test
    void supportsSeparateSetLabelAndDoesNotCreateAPriceFromGiftQuantities() {
        var draft = parser.parse("YES24\nStandard VER.\nเดี่ยว 590\nSET(3)\nราคา 1750 (มัดจำ900)");
        assertThat(draft.versions().getFirst().groups()).extracting(group -> group.name()).containsExactly("เดี่ยว", "SET(3)");
        assertThat(draft.versions().getFirst().groups().get(1).price()).isEqualByComparingTo("1750");
        assertThat(parser.parse("Pre-order gift\nSet Buy 3 albums get 3 cards").versions()).isEmpty();
    }

    @Test
    void handlesObservedOcrErrorsInSingleAndSetLabels() {
        var draft = parser.parse("ENGENE VER.\nบัมละ 400 (มัดจำ200)\ndar set(6) 2,390 (มัดจำ1,200)\nniht set(6) 2,390 (มัดจำ1,200)");
        assertThat(draft.versions().getFirst().groups()).extracting(group -> group.name()).containsExactly("สุ่ม", "DAY SET(6)", "NIGHT SET(6)");
        var set = parser.parse("PHOTOBOOK VER.\n$[7(3) 1,750 (มัดจำ900)").versions().getFirst().groups().getFirst();
        assertThat(set.name()).isEqualTo("SET(3)");
        assertThat(set.price()).isEqualByComparingTo("1750");
        var album = parser.parse("Weverse Albums VER.\nบมละ 300 (มดจา 150)").versions().getFirst().groups().getFirst();
        assertThat(album.name()).isEqualTo("เดี่ยว");
        assertThat(album.deposit()).isEqualByComparingTo("150");
    }

    @Test
    void damagedSetPrefixCannotMakeQuantityThePriceOrHideUnreadableMoney() {
        var group = parser.parse("PHOTOBOOK VER.\nS E 7 (3) 1,750 (มัดจำ900)").versions().getFirst().groups().getFirst();
        assertThat(group.price()).isEqualByComparingTo("1750");
        assertThat(group.name()).isEqualTo("SET(3)");
        assertThat(parser.parse("PHOTOBOOK VER.\nS E 7 (3) 1,75O (มัดจำ900)").versions()).isEmpty();
    }
    private final ProductImageTextParser parser = new ProductImageTextParser();

    @Test
    void extractsAnnouncementWithoutInventingDeskWebsitesOrSevenCardVariants() {
        var draft = parser.parse("""
                สั่ง T-SHIRT VER. 1 กล่อง รับการ์ด 1 ชุด (7 ใบ)
                Ktown4u / YES24
                T-SHIRT VER.
                กล่องละ 1,350 (มัดจำ 700)
                Aladin / Makestar / Plus Chat / Mnet Plus Merch
                T-SHIRT VER.
                กล่องละ 1,450 (มัดจำ 700)
                DESK VER.
                กล่องละ 950 (มัดจำ 500)
                """);
        assertThat(draft.versions()).hasSize(2);
        var shirt = draft.versions().getFirst();
        assertThat(shirt.name()).isEqualTo("T-SHIRT VER.");
        assertThat(shirt.groups()).hasSize(2);
        assertThat(shirt.groups().getFirst().websites()).containsExactly("Ktown4u", "YES24");
        assertThat(shirt.groups().getFirst().price()).isEqualByComparingTo("1350");
        assertThat(shirt.groups().get(1).websites()).containsExactly("Aladin", "Makestar", "Plus Chat", "Mnet Plus Merch");
        var desk = draft.versions().get(1).groups().getFirst();
        assertThat(desk.websites()).containsExactly(WebsiteQuickAddService.NO_WEBSITE);
        assertThat(desk.price()).isEqualByComparingTo("950");
        assertThat(desk.deposit()).isEqualByComparingTo("500");
        assertThat(draft.warnings()).anyMatch(message -> message.contains("DESK") && message.contains("ไม่มีเว็บ"));
        assertThat(draft.productName()).isNull();
    }

    @Test
    void acceptsLowercaseOcrTextAndDecimalMoney() {
        var draft = parser.parse("ktown4u yes24\nt-shirt ver\nกล่องละ 1,350.75 (มัดจำ700.10)");
        var group = draft.versions().getFirst().groups().getFirst();
        assertThat(group.price()).isEqualByComparingTo(new BigDecimal("1350.75"));
        assertThat(group.deposit()).isEqualByComparingTo(new BigDecimal("700.10"));
    }

    @Test
    void defaultsAbsentDepositToZeroButKeepsStatedUnreadableDepositForReview() {
        var draft = parser.parse("YES24\nDESK VER.\nราคา 950 บาท");
        assertThat(draft.versions().getFirst().groups().getFirst().deposit()).isEqualByComparingTo("0");
        assertThat(draft.warnings()).noneMatch(message -> message.contains("ไม่พบยอดมัดจำ"));
        var unreadable=parser.parse("YES24\nDESK VER.\nราคา950บาท (มัดจำอ่านไม่ชัด)");
        assertThat(unreadable.versions().getFirst().groups().getFirst().deposit()).isNull();
    }

    @Test
    void keepsDepositAndLeavesConflictingPriceEmptyForReview() {
        var draft = parser.parse("YES24\nDESK VER.\nกล่องละ 950 (มัดจำ 1000)");
        assertThat(draft.versions().getFirst().groups().getFirst().price()).isNull();
        assertThat(draft.versions().getFirst().groups().getFirst().deposit()).isEqualByComparingTo("1000");
    }

    @Test
    void truncatedSinglePriceCannotSilentlyDiscardTheDepositAndBecomeAValidOffer() {
        var draft = parser.parse("Weverse Shop\nPHOTOBOOK VER.\nบั้มละ 90 (มัดจำ300)");
        var offer = draft.versions().getFirst().groups().getFirst();
        assertThat(offer.price()).isNull();
        assertThat(offer.deposit()).isEqualByComparingTo("300");
        assertThat(draft.warnings()).anyMatch(message -> message.contains("90") && message.contains("300"));
    }

    @Test
    void preservesUnfamiliarShopNamesForManualMapping() {
        var draft = parser.parse("Ktown4u / My Music Shop\nStandard VER.\nราคา 1000 (มัดจำ 500)");
        assertThat(draft.versions().getFirst().groups().getFirst().websites()).containsExactly("Ktown4u", "My Music Shop");
    }

    @Test
    void doesNotInterpretGiftQuantitiesOrPhotosAsPrices() {
        var draft = parser.parse("T-SHIRT VER.\nสั่ง 1 กล่อง รับการ์ด 1 ชุด (7 ใบ)\n2027 Season Greetings");
        assertThat(draft.versions()).isEmpty();
    }

    @Test
    void doesNotReuseWebsitesSeparatedByUnrelatedText() {
        var draft = parser.parse("YES24\nของแถมพิเศษ\nDESK VER.\nกล่องละ 950 (มัดจำ500)");
        assertThat(draft.versions().getFirst().groups().getFirst().websites()).containsExactly(WebsiteQuickAddService.NO_WEBSITE);
    }

    @Test
    void doesNotUseDepositOnlyLineAsSellingPrice() {
        var draft = parser.parse("YES24\nDESK VER.\nมัดจำ 500");
        assertThat(draft.versions()).isEmpty();
    }

    @Test
    void readsProductNameOnlyWhenExplicitlyLabeled() {
        assertThat(parser.parse("ชื่อสินค้า: ALD1 Garage").productName()).isEqualTo("ALD1 Garage");
        assertThat(parser.parse("Unclear decorative text").productName()).isNull();
    }

    @Test
    void boundsCorrectedTextSize() {
        assertThatThrownBy(() -> parser.parse("a".repeat(30_001))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void doesNotTurnNegativeOrUnclearPriceTokensIntoPositivePartialAmounts() {
        assertThat(parser.parse("YES24\nDESK VER.\nกล่องละ -950 (มัดจำ500)").versions()).isEmpty();
        assertThat(parser.parse("YES24\nDESK VER.\nกล่องละ 1,35O (มัดจำ700)").versions()).isEmpty();
    }

    @Test
    void parsesCroppedWebsitePricesWithoutRequiringAVersionHeading() {
        var draft = parser.parse("""
                🎁YES24 / FNC STORE
                Plus Chat / Mnet Plus Merch
                กล่องละ 1,390 (มัดจำ 700)

                เว็บ Ktown4u ไม่มี POB
                กล่องละ 1,290 (มัดจำ 600)
                """);
        assertThat(draft.versions()).hasSize(1);
        var version = draft.versions().getFirst();
        assertThat(version.name()).isEqualTo("Standard");
        assertThat(version.groups()).hasSize(2);
        assertThat(version.groups().getFirst().websites()).containsExactly("YES24", "FNC STORE", "Plus Chat", "Mnet Plus Merch");
        assertThat(version.groups().getFirst().price()).isEqualByComparingTo("1390");
        assertThat(version.groups().getFirst().deposit()).isEqualByComparingTo("700");
        assertThat(version.groups().get(1).websites()).containsExactly("Ktown4u");
        assertThat(version.groups().get(1).price()).isEqualByComparingTo("1290");
        assertThat(version.groups().get(1).deposit()).isEqualByComparingTo("600");
        assertThat(draft.warnings()).anyMatch(message -> message.contains("Standard"))
                .anyMatch(message -> message.contains("Ktown4u") && message.contains("ไม่มี POB"));
    }

    @Test
    void priceOnlyCropRemainsUsableWhileMissingWebsiteIsFlaggedForReview() {
        var draft = parser.parse("กล่องละ 1,390 (มัดจำ700)");
        assertThat(draft.versions().getFirst().name()).isEqualTo("Standard");
        assertThat(draft.versions().getFirst().groups().getFirst().websites()).containsExactly(WebsiteQuickAddService.NO_WEBSITE);
        assertThat(draft.versions().getFirst().groups().getFirst().price()).isEqualByComparingTo("1390");
        assertThat(draft.warnings()).anyMatch(message -> message.contains("ไม่มีเว็บ"));
    }

    @Test
    void laterVersionHeadingDoesNotInheritThePreviousUnversionedWebsites() {
        var draft = parser.parse("YES24\nกล่องละ 1,390 (มัดจำ700)\nDESK VER.\nกล่องละ 950 (มัดจำ500)");
        assertThat(draft.versions()).extracting(version -> version.name()).containsExactly("Standard", "DESK VER.");
        assertThat(draft.versions().get(1).groups().getFirst().websites()).containsExactly(WebsiteQuickAddService.NO_WEBSITE);
    }

    @Test
    void arbitraryNonPriceTextDoesNotCreateAGenericOffer() {
        assertThat(parser.parse("YES24 / FNC STORE\nเปิดรับ 2027 Season Greetings\nมัดจำ 700").versions()).isEmpty();
    }
}
