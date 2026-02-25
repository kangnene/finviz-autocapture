import com.microsoft.playwright.*;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.io.File;

public class FinvizCapture {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            
            // Finviz Map 접속 (S&P 500 맵)
            page.navigate("https://finviz.com/map.ashx?t=sec");
            
            // 맵 로딩을 위한 충분한 대기 시간 (7초)
            page.waitForTimeout(7000);
            
            // 저장할 폴더 생성
            new File("screenshots").mkdirs();
            
            String fileName = "finviz_" + LocalDate.now() + ".jpg";
            
            // 캡쳐 실행
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("screenshots/" + fileName)));
            
            browser.close();
            System.out.println("성공적으로 캡쳐되었습니다: " + fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
