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
                    .setViewportSize(2560, 1440)
                    .setDeviceScaleFactor(2.0)
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            );

            // 자동화 흔적 제거 스크립트
            context.addInitScript("Object.defineProperty(navigator, 'webdriver', { get: () => undefined })");

            Page page = context.newPage();

            // ==============================
            // 1️⃣ Finviz 캡쳐 (기존 로직 + 강력한 팝업 제거)
            // ==============================
            System.out.println("Finviz 접속 중 (NY Time: " + now + ")...");
            try {
                page.navigate("https://finviz.com/map.ashx?t=sec&st=", 
                    new Page.NavigateOptions().setTimeout(60000));
            
                // 1. [유지] 지도의 캔버스 요소가 나타날 때까지 대기 (데이터 로딩 확인)
                page.waitForSelector("canvas", new Page.WaitForSelectorOptions().setTimeout(10000));
            
                // 2. [추가] 팝업 요소를 브라우저에서 강제로 삭제 (가장 확실함)
                // 이미지 속 'X' 버튼이나 모달 배경을 아예 날려버립니다.
                page.evaluate("() => {" +
                    "  const targets = ['.modal-container', '[class*=\"interstitial\"]', '[id*=\"pro-popup\"]', '.overlay'];" +
                    "  targets.forEach(selector => {" +
                    "    document.querySelectorAll(selector).forEach(el => el.remove());" +
                    "  });" +
                    "  document.body.style.filter = 'none';" + // 배경 흐림 제거
                    "  document.body.style.overflow = 'auto';" + // 스크롤 잠금 해제
                    "}");
            
                // 3. [유지/보강] 마우스 움직임 및 Escape 입력
                page.mouse().move(100, 100); 
                page.keyboard().press("Escape");
            
                // 4. [추가] 팝업의 'X' 버튼을 직접 찾아서 클릭 시도 (클래스명 보강)
                try {
                    // 이미지에서 보이는 우측 상단 X 버튼의 일반적인 패턴들
                    page.locator("div[class*='close'], svg[class*='close'], .icon-close").first().click(
                        new Locator.ClickOptions().setTimeout(2000)
                    );
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
                        new Page.NavigateOptions().setTimeout(120000));
                
                page.waitForSelector(".chart-container", new Page.WaitForSelectorOptions().setTimeout(20000));

                // 팝업 제거
                page.addStyleTag(new Page.AddStyleTagOptions()
                    .setContent(".tv-dialog__close, .js-dialog__close, div[class*='overlap-manager'], [class*='dialog'], [class*='overlay'] { display: none !important; }"));
                page.keyboard().press("Escape");
                page.waitForTimeout(1000);

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

                page.waitForTimeout(5000);
                page.mouse().move(0, 0);
                page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(tradingviewFile))
                    .setType(ScreenshotType.JPEG)
                    .setQuality(100));
                
                System.out.println("트레이딩뷰 캡쳐 완료: " + tradingviewFile);

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
