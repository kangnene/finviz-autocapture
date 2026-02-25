import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ScreenshotType;
import java.nio.file.Paths;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FinvizCapture {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {

            // ✅ 파일명용 현재 시간 생성
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm");
            String now = LocalDateTime.now().format(formatter);

            String finvizFile = "screenshots/finviz_" + now + ".jpg";
            String tradingviewFile = "screenshots/tradingview_" + now + ".jpg";

            // 1. 브라우저 실행
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true)
            );

            // 2. 브라우저 컨텍스트 설정
            BrowserContext context = browser.newContext(
                new Browser.NewContextOptions()
                    .setViewportSize(2560, 1440)
                    .setDeviceScaleFactor(2.0)
                    .setUserAgent(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                    )
            );

            // 자동화 흔적 제거
            context.addInitScript(
                "Object.defineProperty(navigator, 'webdriver', { get: () => undefined })"
            );

            Page page = context.newPage();

            // 📁 폴더 생성
            new File("screenshots").mkdirs();

            // ==============================
            // 1️⃣ Finviz 캡쳐
            // ==============================
            System.out.println("Finviz 접속 중...");
            try {
                page.navigate("https://finviz.com/map.ashx?t=sec",
                        new Page.NavigateOptions().setTimeout(60000));

                page.waitForTimeout(5000);
                page.mouse().move(500, 500);
                page.waitForTimeout(1000);
                page.mouse().wheel(0, 300);
                page.waitForTimeout(9000);

                page.mouse().move(-100, -100);
                page.keyboard().press("Escape");
                page.waitForTimeout(500);

                page.addStyleTag(new Page.AddStyleTagOptions()
                    .setContent("*[class*='tooltip'], *[id*='tooltip'] { display: none !important; }"));

                page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(finvizFile))
                    .setType(ScreenshotType.JPEG)
                    .setQuality(100)
                    .setFullPage(true));

                System.out.println("Finviz 캡쳐 완료: " + finvizFile);

            } catch (Exception e) {
                System.out.println("Finviz 접속 실패, 하지만 계속 진행합니다.");
            }

            // ==============================
            // 2️⃣ TradingView 캡쳐
            // ==============================
            System.out.println("트레이딩뷰 접속 중...");
            try {
                // 1분 봉 차트 접속 (중복 제거 및 로딩 시간 최적화)
                page.navigate("https://www.tradingview.com/chart/?symbol=NASDAQ:NDX&interval=1",
                        new Page.NavigateOptions().setTimeout(120000));
                
                // 충분히 로딩 대기
                page.waitForTimeout(15000);

                // 1. 팝업 광고 및 로그인 창 즉시 제거
                page.addStyleTag(new Page.AddStyleTagOptions()
                    .setContent(".tv-dialog__close, .js-dialog__close, div[class*='overlap-manager'], [class*='dialog'], [class*='overlay'] { display: none !important; }"));
                page.keyboard().press("Escape");

                // 2. '1일' 범위 버튼 클릭 시도 (오늘 전체 보기)
                try {
                    if (page.isVisible("button[data-value='1D']")) {
                        page.click("button[data-value='1D']");
                    } else {
                        for (int i = 0; i < 8; i++) page.keyboard().press("Control+ArrowDown");
                    }
                    page.waitForTimeout(5000); // 정렬될 시간 넉넉히 주기
                } catch (Exception e) {
                    System.out.println("범위 버튼 클릭 실패, 수동 축소 모드 작동");
                    for (int i = 0; i < 5; i++) page.keyboard().press("Control+ArrowDown");
                }

                // 3. 마지막 캡쳐 전 마우스 치우기
                page.mouse().move(0, 0);
                
                // 캡쳐 실행 (선언해둔 변수 사용)
                page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(tradingviewFile))
                    .setType(ScreenshotType.JPEG)
                    .setQuality(100));
                
                System.out.println("트레이딩뷰 캡쳐 완료: " + tradingviewFile);

            } catch (Exception e) {
                System.out.println("트레이딩뷰 캡쳐 중 오류 발생");
                e.printStackTrace();
            }

            browser.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
