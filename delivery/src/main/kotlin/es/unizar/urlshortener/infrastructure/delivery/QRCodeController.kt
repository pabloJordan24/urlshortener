import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping


@Controller
class QRCodeController {

    @RequestMapping("qr")
    fun firstEndpoint(model: MutableMap<String, Any>): String {
        return "qr"

    }
}

