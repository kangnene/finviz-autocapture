import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ScreenshotType;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.io.File;

public class FinvizCapture {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            // 1. 브라우저 실행: 서버 환경을 위해 headless는 true 유지
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            
            // 2. 브라우저 컨텍스트 설정 (화질 개선 + 사람인 척 하기)
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(2560, 1440)   // 전체적인 해상도 상향 (고화질)
                .setDeviceScaleFactor(2.0)     // 픽셀 밀도 2배 (직접 찍는 것보다 선명하게)
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"));

            // 🔥 Finviz 보안 우회를 위한 자동화 흔적 제거 스크립트
            context.addInitScript("Object.defineProperty(navigator, 'webdriver', { get: () => undefined })");

            
            // 폴더 생성
            Page page = context.newPage();
            new File("screenshots").mkdirs();
            String date = LocalDate.now().toString();

            // --- 1. Finviz 섹터 맵 캡쳐 ---
            System.out.println("Finviz 접속 중...");
            page.navigate("https://finviz.com/map.ashx?t=sec");

            // Finviz 전용: 사람처럼 행동하는 척 해서 보안망 통과
            page.waitForTimeout(5000); // 우선 대기
            page.mouse().move(500, 500); // 마우스 커서 올리기
            page.waitForTimeout(1000);
            page.mouse().wheel(0, 300);  // 살짝 스크롤
            page.waitForTimeout(9000);  // 나머지 로딩 대기 (총 15초 수준)
            page.mouse().move(-100, -100);  // 마우스를 왼쪽 상단으로 이동 (툴팁 제거)
            page.keyboard().press("Escape"); // 혹시 남아있으면 닫기
            page.waitForTimeout(500);
            page.addStyleTag(new Page.AddStyleTagOptions()
                .setContent("*[class*='tooltip'], *[id*='tooltip'] { display: none !important; }"));
            
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("screenshots/finviz_" + date + ".jpg"))
                .setType(ScreenshotType.JPEG)
                .setQuality(100)
                .setFullPage(true));

            // --- 2. 트레이딩뷰 나스닥 차트 캡쳐 ---
            System.out.println("트레이딩뷰 접속 중...");
            page.navigate("https://www.tradingview.com/chart/ZZfo1QcX/?symbol=NASDAQ%3ANDX");
            
            // 차트 캔버스가 무거우므로 로딩 대기를 15초로 넉넉히 설정
            // 불필요한 레이어(팝업 등)가 로드될 시간을 고려합니다.
            page.waitForTimeout(15000); 

            // 디버그용 전체 캡쳐
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("screenshots/debug_tradingview.jpg"))
                .setFullPage(true));
            
            // 차트 화면이 완전히 렌더링되도록 특정 요소(차트 컨테이너)가 보일 때까지 대기하는 로직을 넣으면 더 정확합니다.
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("screenshots/tradingview_ndx_" + date + ".jpg"))
                .setType(ScreenshotType.JPEG)
                .setQuality(100));
            System.out.println("트레이딩뷰 캡쳐 완료.");

            browser.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
