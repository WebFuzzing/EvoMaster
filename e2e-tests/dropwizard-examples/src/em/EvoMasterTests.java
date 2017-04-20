import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertNotNull;


public class EvoMasterTests {

    private static com.foo.rest.examples.dw.positiveinteger.PIController controller = new com.foo.rest.examples.dw.positiveinteger.PIController();
    private static String baseUrlOfSut;


    @BeforeAll
    public static void initClass() {
        baseUrlOfSut = controller.startSut();
        assertNotNull(baseUrlOfSut);
    }


    @AfterAll
    public static void tearDown() {
        controller.stopSut();
    }


    @BeforeEach
    public void initTest() {
        controller.resetStateOfSUT();
    }


}