import com.microsoft.playwright.*;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.io.File;

public class FinvizCapture {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {

            // 브라우저 실행 (화면 크기를 QHD + 고밀도 설정)
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                    .setHeadless(false)   // 🔥 Finviz 차단 대응
                    .setSlowMo(80)
            );

            BrowserContext context = browser.newContext(
                new Browser.NewContextOptions()
                    .setViewportSize(2560, 1440)   // 🔥 QHD 해상도
                    .setDeviceScaleFactor(2.0)     // 🔥 픽셀 밀도 2배
                    .setUserAgent(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/122.0.0.0 Safari/537.36"
                    )
            );

            // 🔥 webdriver 제거
            context.addInitScript(
                "Object.defineProperty(navigator, 'webdriver', { get: () => undefined })"
            );

            Page page = context.newPage();
            
            // 폴더 생성
            new File("screenshots").mkdirs();
            String date = LocalDate.now().toString();

            // --- 1. Finviz 섹터 맵 캡쳐 ---
            System.out.println("Finviz 접속 중...");
            page.navigate("https://finviz.com/map.ashx?t=sec");

            // 사람처럼 행동
            page.waitForTimeout(4000);
            page.mouse().move(600, 500);
            page.waitForTimeout(1200);
            page.mouse().wheel(0, 800);
            page.waitForTimeout(1500);
            page.mouse().move(1200, 400);
            page.waitForTimeout(3000);

            page.waitForTimeout(10000); // 기존 10초 유지

            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("screenshots/finviz_" + date + ".jpg"))
                .setType(ScreenshotType.JPEG)
                .setQuality(100));   // 🔥 최고 화질

            System.out.println("Finviz 캡쳐 완료.");

            // --- 2. 트레이딩뷰 나스닥 차트 캡쳐 ---
            System.out.println("트레이딩뷰 접속 중...");
            page.navigate("https://www.tradingview.com/chart/?symbol=NASDAQ%3ANDX");
            
            // 차트 캔버스가 무거우므로 로딩 대기를 15초로 넉넉히 설정
            // 불필요한 레이어(팝업 등)가 로드될 시간을 고려합니다.
            page.waitForTimeout(15000); 
            
            // 차트 화면이 완전히 렌더링되도록 특정 요소(차트 컨테이너)가 보일 때까지 대기하는 로직을 넣으면 더 정확합니다.
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("screenshots/tradingview_ndx_" + date + ".jpg")));
            System.out.println("트레이딩뷰 캡쳐 완료.");

            browser.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
