import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ScreenshotType;
import java.nio.file.Paths;
import java.io.File;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class FinvizCapture {
    public static void main(String[] args) {
        
        // ⭐ [설정] 직접 Run 시 모든 종목을 다 찍으려면 true
        // 자동 스케줄러(오전 11시) 등에서 Finviz/NDX만 찍으려면 false로 변경하세요.
        boolean captureAll = true; 

        try (Playwright playwright = Playwright.create()) {

            // ✅ 1. 미국 동부 시간(보스턴/뉴욕)으로 파일명용 시간 생성
            ZonedDateTime nowNY = ZonedDateTime.now(ZoneId.of("America/New_York"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm");
            String now = nowNY.format(formatter);

            // 📁 screenshots 폴더 생성 (없으면 생성)
            new File("screenshots").mkdirs();

            // 2. 브라우저 실행
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true)
            );

            // 3. 브라우저 컨텍스트 설정
            BrowserContext context = browser.newContext(
                new Browser.NewContextOptions()
                    .setViewportSize(2560, 1440)
                    .setDeviceScaleFactor(2.0)
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            );

            // 자동화 흔적 제거
            context.addInitScript("Object.defineProperty(navigator, 'webdriver', { get: () => undefined })");

            Page page = context.newPage();

            // ============================================================
            // 1️⃣ Finviz 캡쳐 (항상 실행 + 팝업 파괴 로직 통합)
            // ============================================================
            String finvizFile = "screenshots/" + now + "_finviz.jpg"; // 날짜가 맨 앞으로
            System.out.println("Finviz 접속 중 (NY Time: " + now + ")...");
            
            try {
                page.navigate("https://finviz.com/map.ashx?t=sec&st=", 
                    new Page.NavigateOptions().setTimeout(60000));

                // 지도의 캔버스 요소 대기
                page.waitForSelector("canvas", new Page.WaitForSelectorOptions().setTimeout(10000));

                // 팝업 요소 강제 삭제 및 배경 흐림 해제 (JS 실행)
                page.evaluate("() => {" +
                    "  const targets = ['.modal-container', '[class*=\"interstitial\"]', '[id*=\"pro-popup\"]', '.overlay'];" +
                    "  targets.forEach(selector => {" +
                    "    document.querySelectorAll(selector).forEach(el => el.remove());" +
                    "  });" +
                    "  document.body.style.filter = 'none';" +
                    "  document.body.style.overflow = 'auto';" +
                    "}");

                page.mouse().move(100, 100); 
                page.keyboard().press("Escape");

                // 추가적인 X 버튼 클릭 시도
                try {
                    page.locator("div[class*='close'], svg[class*='close'], .icon-close").first().click(
                        new Locator.ClickOptions().setTimeout(2000)
                    );
                } catch (Exception ignored) {}

                // 광고 제거 CSS
                page.addStyleTag(new Page.AddStyleTagOptions()
                    .setContent(
                        "div[class*='interstitial'], div[class*='overlay'], [id*='pro-popup'] { display: none !important; }" +
                        "body, .map-container { filter: none !important; transition: none !important; }"
                    ));

                page.waitForTimeout(500); 
                page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(finvizFile))
                    .setType(ScreenshotType.JPEG)
                    .setQuality(90)
                    .setFullPage(true));

                System.out.println("Finviz 캡쳐 완료: " + finvizFile);

            } catch (Exception e) {
                System.out.println("Finviz 접속 실패! 에러: " + e.getMessage());
                page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("screenshots/" + now + "_error_finviz.jpg")));
            }

            // ============================================================
            // 2️⃣ TradingView 및 개별 종목 캡쳐 (조건부 실행)
            // ============================================================
            
            // 📍 캡처할 전체 종목 리스트
            String[] stockList = {"NASDAQ:NDX", "XWEL", "SGN", "RETO", "FRGT", "RCKY", "LRMR", "CONL", "MSTX", "CDIO"};
            
            for (String symbol : stockList) {
                
                // ⭐ [조건 검사] 
                // NDX가 아니면서 captureAll이 false이면 건너뜀
                if (!symbol.equals("NASDAQ:NDX") && !captureAll) {
                    continue; 
                }

                System.out.println(symbol + " 차트 접속 중...");
                try {
                    page.navigate("https://www.tradingview.com/chart/?symbol=" + symbol + "&interval=1",
                            new Page.NavigateOptions().setTimeout(120000));
                    
                    page.waitForSelector(".chart-container", new Page.WaitForSelectorOptions().setTimeout(20000));

                    // 팝업 제거 CSS 주입
                    page.addStyleTag(new Page.AddStyleTagOptions()
                        .setContent(".tv-dialog__close, .js-dialog__close, div[class*='overlap-manager'], [class*='dialog'], [class*='overlay'] { display: none !important; }"));
                    page.keyboard().press("Escape");
                    page.waitForTimeout(1000);

                    // '1일' 범위(1D) 클릭 시도
                    try {
                        Locator btn1D = page.locator("button[data-value='1D'], [data-name='1D']").first();
                        if (btn1D.isVisible()) {
                            btn1D.click(new Locator.ClickOptions().setForce(true));
                        } else {
                            page.locator("span:has-text('1D'), div:has-text('1D')").last().click(new Locator.ClickOptions().setForce(true));
                        }
                    } catch (Exception e) {
                        for (int i = 0; i < 5; i++) {
                            page.keyboard().press("Control+ArrowDown");
                            page.waitForTimeout(100);
                        }
                    }

                    page.waitForTimeout(3000); 
                    page.mouse().move(0, 0);

                    // ✅ 파일명: 날짜_종목명.jpg (정렬 최적화)
                    String fileName = "screenshots/" + now + "_" + symbol.replace(":", "_") + ".jpg";
                    
                    page.screenshot(new Page.ScreenshotOptions()
                        .setPath(Paths.get(fileName))
                        .setType(ScreenshotType.JPEG)
                        .setQuality(100));
                    
                    System.out.println(symbol + " 캡쳐 완료: " + fileName);

                } catch (Exception e) {
                    System.out.println(symbol + " 처리 중 오류 발생: " + e.getMessage());
                }
            }

            browser.close();
            System.out.println("--- 모든 작업 완료 ---");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
