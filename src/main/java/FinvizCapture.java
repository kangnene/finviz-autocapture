import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.WaitUntilState; // 추가됨
import java.nio.file.Paths;
import java.io.File;
import java.time.ZonedDateTime; // 변경됨
import java.time.ZoneId; // 추가됨
import java.time.format.DateTimeFormatter;

public class FinvizCapture {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {

            // ✅ 1. 미국 동부 시간(보스턴/뉴욕)으로 파일명 생성
            ZonedDateTime nowNY = ZonedDateTime.now(ZoneId.of("America/New_York"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm");
            String now = nowNY.format(formatter);

            String finvizFile = "screenshots/finviz_" + now + ".jpg";
            String tradingviewFile = "screenshots/tradingview_" + now + ".jpg";

            // 📁 폴더 생성
            new File("screenshots").mkdirs();

            // 2. 브라우저 실행
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true)
            );

            // 3. 브라우저 컨텍스트 설정 (User-Agent를 실제 브라우저와 더 비슷하게 유지)
            BrowserContext context = browser.newContext(
                new Browser.NewContextOptions()
                    .setViewportSize(1920, 1080) // 2560은 너무 무거울 수 있음
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .setLocale("ko-KR")
                    .setTimezoneId("America/New_York")
            );

            // 자동화 흔적 제거 스크립트
            context.addInitScript("Object.defineProperty(navigator, 'webdriver', { get: () => undefined })");

            Page page = context.newPage();

            // ==============================
            // 1️⃣ Finviz 캡쳐 (기존 로직 + 강력한 팝업 제거)
            // ==============================
            System.out.println("Finviz 접속 중 (NY Time: " + now + ")...");
            try {
                // networkidle 대신 domcontentloaded로 기준 완화
                page.navigate("https://finviz.com/map.ashx?t=sec&st=", 
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(60000));
                
                // 폰트가 안 오더라도 10초만 딱 기다리고 진행
                page.waitForTimeout(10000); 
            
                // 스크린샷 옵션 변경 (FullPage 해제)
                page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(finvizFile))
                    .setType(ScreenshotType.JPEG)
                    .setQuality(80)
                    .setFullPage(false)); // FullPage true 시 폰트 대기 때문에 타임아웃 발생함
            } catch (Exception ignored) {
                    // 버튼을 못 찾아도 에러로 중단되지 않게 처리
                }
            
                // 5. [유지] 광고 파괴 CSS 주입
                page.addStyleTag(new Page.AddStyleTagOptions()
                    .setContent(
                        "div[class*='interstitial'], div[class*='overlay'], [id*='pro-popup'] { display: none !important; }" +
                        "body, .map-container { filter: none !important; transition: none !important; }"
                    ));
            
                // 6. [조정] 화면 갱신을 위해 아주 잠깐 대기
                page.waitForTimeout(500); 
            
                // 7. 캡처 실행
                page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(finvizFile))
                    .setType(ScreenshotType.JPEG)
                    .setQuality(90)
                    .setFullPage(true));
            
                System.out.println("Finviz 캡쳐 완료: " + finvizFile);
            
            } catch (Exception e) {
                System.out.println("Finviz 접속 실패! 에러: " + e.getMessage());
                page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("screenshots/error_finviz.jpg")));
            }

            // ==============================
            // 2️⃣ TradingView 캡쳐 (기존 로직 유지 + 타임아웃 보강)
            // ==============================
            System.out.println("트레이딩뷰 접속 중...");
            try {
                page.navigate("https://www.tradingview.com/chart/?symbol=NASDAQ:NDX&interval=1",
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(60000));
                
                page.waitForTimeout(15000); // 차트가 그려질 최소한의 시간 확보
                
                page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(tradingviewFile))
                    .setType(ScreenshotType.JPEG)
                    .setFullPage(false)); 
            } catch (Exception e) {
                System.out.println("트레이딩뷰 오류 발생");
                e.printStackTrace();
            }

            browser.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
