

//Java API
import java.io.File
//External test dependency
import org.junit.jupiter.api.Test
// dependency to current module
import org.evomaster.core.Lazy
// dependency to other module
import org.evomaster.client.java.controller.api.dto.SutInfoDto



class AKotlinClassWithDependencies(){

    @Test
    fun a(){
        val file = File("")
        Lazy.assert{true}
        val dto = SutInfoDto()
    }

}