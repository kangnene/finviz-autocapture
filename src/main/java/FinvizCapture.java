import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.WaitUntilState;
import java.nio.file.Paths;
import java.io.File;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class FinvizCapture {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {

            // 1. 시간 설정 및 파일명 생성
            ZonedDateTime nowNY = ZonedDateTime.now(ZoneId.of("America/New_York"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm");
            String now = nowNY.format(formatter);

            String finvizFile = "screenshots/finviz_" + now + ".jpg";
            String tradingviewFile = "screenshots/tradingview_" + now + ".jpg";

            new File("screenshots").mkdirs();

            // 2. 브라우저 실행
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(java.util.Arrays.asList(
                        "--disable-blink-features=AutomationControlled", // 자동화 흔적 제거
                        "--disable-web-security",
                        "--allow-running-insecure-content"
                    ))
            );

            // 3. 우회 설정 (헤더 및 언어 설정 추가)
            BrowserContext context = browser.newContext(
                new Browser.NewContextOptions()
                    .setViewportSize(1920, 1080)
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .setLocale("ko-KR")
                    .setTimezoneId("America/New_York")
            );

            // 자동화 흔적 제거
            context.addInitScript("Object.defineProperty(navigator, 'webdriver', { get: () => undefined })");

            Page page = context.newPage();

            // ==============================
            // 1️⃣ Finviz 캡쳐 (강화된 우회 로직)
            // ==============================
            System.out.println("Finviz 접속 중...");
            try {
                // 1. 단순 navigate 대신, 응답 코드를 확인하거나 재시도 로직 추가
                Response response = page.navigate("https://finviz.com/map.ashx?t=sec&st=", 
                    new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(60000));
            
                // 만약 403 Forbidden이나 차단 페이지가 떴을 경우를 대비해 5초 더 대기
                page.waitForTimeout(5000);
            
                // 2. [추가] Cloudflare 체크박스나 대기 화면을 피하기 위해 
                // 브라우저가 화면을 그릴 시간을 충분히 줌 (15~20초 권장)
                System.out.println("데이터 로딩 대기 중 (20초)...");
                page.waitForTimeout(15000); 
            
                // 3. [추가] 화면에 아무것도 안 뜰 경우를 대비해 강제로 마우스 클릭/스크롤
                page.mouse().click(500, 500);
                page.keyboard().press("End");
                page.waitForTimeout(2000);
                page.keyboard().press("Home");
            
                // 4. [보강] 팝업 제거 및 배경 흐림 강제 해제
                page.evaluate("() => {" +
                    "  const style = document.createElement('style');" +
                    "  style.innerHTML = ` " +
                    "    .modal-container, [class*='interstitial'], [id*='pro-popup'], .overlay { display: none !important; } " +
                    "    body, .map-container { filter: none !important; opacity: 1 !important; } " +
                    "  `;" +
                    "  document.head.appendChild(style);" +
                    "}");
            
                // 5. 캡처 (Viewport만 찍기)
                page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(finvizFile))
                    .setType(ScreenshotType.JPEG)
                    .setQuality(90)
                    .setFullPage(false));
                
                System.out.println("Finviz 캡쳐 완료!");
            } catch (Exception e) {
                System.out.println("Finviz 실패 원인: " + e.getMessage());
                // 실패 시 현재 화면이라도 찍어서 무엇이 막고 있는지 확인 (디버깅용)
                page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("screenshots/debug_finviz.jpg")));
            }

            // ==============================
            // 2️⃣ TradingView 캡쳐 (우회 로직 적용)
            // ==============================
            System.out.println("트레이딩뷰 접속 중...");
            try {
                page.navigate("https://www.tradingview.com/chart/?symbol=NASDAQ:NDX&interval=1",
                        new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(60000));
                
                // 차트 로딩을 위해 15초 강제 대기
                page.waitForTimeout(15000);
            
                // '1일' 범위(1D) 클릭 시도 (사용자님의 3단계 방어막 유지)
                try {
                    Locator btn1D = page.locator("button[data-value='1D'], [data-name='1D']").first();
                    if (btn1D.isVisible()) {
                        btn1D.click(new Locator.ClickOptions().setForce(true));
                    } else {
                        page.locator("span:has-text('1D'), div:has-text('1D')").last().click(new Locator.ClickOptions().setForce(true));
                    }
                } catch (Exception e) {
                    for (int i = 0; i < 10; i++) {
                        page.keyboard().press("Control+ArrowDown");
                        page.waitForTimeout(200);
                    }
                }
            
                // 1D 버튼 클릭 후 잠시 대기
                page.waitForTimeout(5000); // 대기 시간을 충분히 주어야 1D 데이터가 렌더링됨
            
                // 캡처 실행
                page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(tradingviewFile))
                    .setType(ScreenshotType.JPEG)
                    .setQuality(90) // 품질은 90으로 설정하여 고화질을 유지하면서 파일 크기는 줄임
                    .setFullPage(false)
                    .setDeviceScaleFactor(1.5));  // 해상도를 1.5배로 향상시킴
                
                System.out.println("트레이딩뷰 캡쳐 완료: " + tradingviewFile);
            } catch (Exception e) {
                System.out.println("트레이딩뷰 실패: " + e.getMessage());
            }
            
            browser.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
