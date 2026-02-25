import com.microsoft.playwright.*;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.io.File;

public class FinvizCapture {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            // 브라우저 실행 (화면 크기를 1920x1080으로 고정)
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1920, 1080));
            Page page = context.newPage();
            
            // 폴더 생성
            new File("screenshots").mkdirs();
            String date = LocalDate.now().toString();

            // --- 1. Finviz 섹터 맵 캡쳐 ---
            System.out.println("Finviz 접속 중...");
            page.navigate("https://finviz.com/map.ashx?t=sec");
            page.waitForTimeout(10000); // 10초 대기
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("screenshots/finviz_" + date + ".jpg")));
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
