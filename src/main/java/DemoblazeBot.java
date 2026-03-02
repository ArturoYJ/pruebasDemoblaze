import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class DemoblazeBot {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String BASE_URL = "https://www.demoblaze.com";

    // Selectores centralizados
    private final By HOME_LINK = By.xpath("//a[contains(text(), 'Home')]");
    private final By CART_LINK = By.id("cartur");
    private final By ADD_TO_CART_BTN = By.xpath("//a[text()='Add to cart']");
    private final By CART_TABLE_ROWS = By.xpath("//tbody[@id='tbodyid']/tr");

    public DemoblazeBot() {
        this.driver = new ChromeDriver();
        this.driver.manage().window().maximize();
        this.wait = new WebDriverWait(this.driver, Duration.ofSeconds(10));
    }

    public static void main(String[] args) {
        System.out.println("Iniciando Suite de Pruebas Demoblaze (Standalone)...");
        DemoblazeBot bot = new DemoblazeBot();

        try {
            bot.ejecutarCasosDePrueba();
        } catch (Exception e) {
            System.err.println("\n[ERROR FATAL] La ejecución se detuvo: " + e.getMessage());
        } finally {
            bot.apagarBot();
        }
    }

    // --- ORQUESTADOR DE CASOS DE PRUEBA ---
    public void ejecutarCasosDePrueba() {
        driver.get(BASE_URL);

        // TC-CART-01: Agregar producto
        System.out.println("\n>>> EJECUTANDO TC-CART-01: Agregar un producto");
        agregarProducto("Samsung galaxy s6");
        validarCarrito(1);

        // TC-CART-02: Agregar múltiples productos
        System.out.println("\n>>> EJECUTANDO TC-CART-02: Agregar múltiples productos");
        agregarProducto("Nokia lumia 1520");
        validarCarrito(2); // Validamos que ahora haya 2

        // TC-CART-04: Persistencia del carrito
        System.out.println("\n>>> EJECUTANDO TC-CART-04: Persistencia del carrito (Refresh)");
        validarPersistencia(2); // Recargamos y verificamos que sigan los 2

        // TC-CART-03: Eliminar producto
        System.out.println("\n>>> EJECUTANDO TC-CART-03: Eliminar producto");
        eliminarProducto();
        validarCarrito(1); // Validamos que haya desaparecido uno

        System.out.println("\n=======================================================");
        System.out.println("TODOS LOS CASOS DE PRUEBA EJECUTADOS CORRECTAMENTE.");
        System.out.println("=======================================================\n");
    }

    // --- LÓGICA DE NEGOCIO E INTERACCIÓN ---

    private void agregarProducto(String nombreProducto) {
        System.out.println("Agregando producto: " + nombreProducto);

        // Volver al Home usando espera para evitar clics en elementos no listos
        wait.until(ExpectedConditions.elementToBeClickable(HOME_LINK)).click();

        // Encontrar producto y hacer clic
        By productLink = By.xpath("//a[contains(text(), '" + nombreProducto + "')]");
        wait.until(ExpectedConditions.elementToBeClickable(productLink)).click();

        // Añadir al carrito
        wait.until(ExpectedConditions.elementToBeClickable(ADD_TO_CART_BTN)).click();

        // Validar Alerta (Parte de los requisitos del TC-01)
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        System.out.println("Validación: Alerta presente ('" + alert.getText() + "')");
        alert.accept();
    }

    private void validarCarrito(int cantidadEsperada) {
        // Navegación inteligente: Solo ir al carrito si no estamos en él
        if (!driver.getCurrentUrl().contains("cart.html")) {
            wait.until(ExpectedConditions.elementToBeClickable(CART_LINK)).click();
        }

        try {
            List<WebElement> cartItems = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(CART_TABLE_ROWS));

            if (cartItems.size() == cantidadEsperada) {
                System.out.println("Validación Exitosa: Hay exactamente " + cantidadEsperada + " producto(s) en el carrito.");
            } else {
                throw new RuntimeException("Fallo de validación. Se esperaban " + cantidadEsperada + " pero hay " + cartItems.size());
            }
        } catch (org.openqa.selenium.TimeoutException e) {
            if (cantidadEsperada > 0) {
                throw new RuntimeException("Fallo de validación. El carrito está vacío pero se esperaban " + cantidadEsperada);
            } else {
                System.out.println("Validación Exitosa: El carrito está vacío.");
            }
        }
    }

    private void eliminarProducto() {
        if (!driver.getCurrentUrl().contains("cart.html")) {
            wait.until(ExpectedConditions.elementToBeClickable(CART_LINK)).click();
        }

        try {
            WebElement rowToDelete = wait.until(ExpectedConditions.presenceOfElementLocated(CART_TABLE_ROWS));
            WebElement deleteBtn = rowToDelete.findElement(By.xpath(".//a[text()='Delete']"));

            deleteBtn.click();
            System.out.println("Clic en eliminar. Esperando a que el DOM se actualice...");

            // Validación rigurosa de desaparición (Staleness)
            boolean isDeleted = wait.until(ExpectedConditions.stalenessOf(rowToDelete));
            if (isDeleted) {
                System.out.println("Validación Exitosa: El producto desapareció del DOM correctamente.");
            }
        } catch (Exception e) {
            throw new RuntimeException("No se pudo eliminar el producto. Error: " + e.getMessage());
        }
    }

    private void validarPersistencia(int cantidadEsperada) {
        if (!driver.getCurrentUrl().contains("cart.html")) {
            wait.until(ExpectedConditions.elementToBeClickable(CART_LINK)).click();
        }

        System.out.println("Ejecutando F5 (Refresh) en el navegador...");
        // Esto destruye el DOM y obliga a JavaScript a leer el LocalStorage de nuevo
        driver.navigate().refresh();

        System.out.println("Página recargada. Verificando estado...");
        // Reutilizamos nuestro método existente para validar
        validarCarrito(cantidadEsperada);
    }

    private void apagarBot() {
        if (this.driver != null) {
            this.driver.quit();
            System.out.println("Recursos liberados. Sesión finalizada.");
        }
    }
}