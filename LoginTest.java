package Login;

import java.io.FileInputStream;
import java.sql.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.*;

public class LoginTest {

    WebDriver driver;
    Connection con;

    @BeforeClass
    public void setUp() {
        setupChromeDriver();
        setupDatabaseConnection();
        driver.get("http://localhost/myproject/login.php");
    }

    // ✅ Separate method to initialize Chrome WebDriver
    public void setupChromeDriver() {
        try {
            System.setProperty("webdriver.chrome.driver", "E://chromedriver-win64//chromedriver-win64//chromedriver.exe");
            driver = new ChromeDriver();
            driver.manage().window().maximize();
        } catch (Exception e) {
            System.out.println("❌ Failed to initialize ChromeDriver.");
            e.printStackTrace();
        }
    }

    // ✅ Separate method to connect to MySQL
    public void setupDatabaseConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/mydb", "root", "");
        } catch (Exception e) {
            System.out.println("❌ Failed to connect to MySQL.");
            e.printStackTrace();
        }
    }

    @Test
    public void loginFromExcelAndVerifyDB() {
        try (FileInputStream file = new FileInputStream("E:\\EclipseIDE\\DemoTester\\LoginData.xlsx");
             Workbook workbook = new XSSFWorkbook(file)) {

            Sheet sheet = workbook.getSheetAt(0);

            // Skip header and loop through remaining rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String id = row.getCell(0).toString().replace(".0", "");
                String name = row.getCell(1).toString();
                String email = row.getCell(2).toString();

                // Fill and submit the form
                driver.findElement(By.name("id")).clear();
                driver.findElement(By.name("id")).sendKeys(id);

                driver.findElement(By.name("name")).clear();
                driver.findElement(By.name("name")).sendKeys(name);

                driver.findElement(By.name("email")).clear();
                driver.findElement(By.name("email")).sendKeys(email);

                driver.findElement(By.xpath("/html/body/div/form/input[4]")).click();

                driver.navigate().back();

                // DB validation
                String query = "SELECT * FROM users WHERE id = ?";
                try (PreparedStatement stmt = con.prepareStatement(query)) {
                    stmt.setString(1, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String dbName = rs.getString("name");
                            String dbEmail = rs.getString("email");

                            if (name.equals(dbName) && email.equals(dbEmail)) {
                                System.out.println("✅ Row " + i + " validated successfully.");
                            } else {
                                System.out.println("❌ Row " + i + " mismatch! Excel vs DB.");
                            }
                        } else {
                            System.out.println("⚠️ Row " + i + ": No record found in DB.");
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public void tearDown() {
        try {
            if (driver != null) driver.quit();
            if (con != null) con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
