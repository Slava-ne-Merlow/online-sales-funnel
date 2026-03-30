package de.vyacheslav.kushchenko.sales.funnel.infrastructure.mail

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.vyacheslav.kushchenko.sales.funnel.infrastructure.mail.MailProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

@Service
@ConditionalOnProperty(name = ["app.mail.provider"], havingValue = "python", matchIfMissing = true)
class PythonMailService(
    @Value("\${app.mail.python.command:python3}") private val pythonCmd: String,
    @Value("\${app.mail.python.script:./mail/send_mail.py}") private val scriptPath: String,
    @Value("\${app.mail.python.timeoutSeconds:20}") private val timeoutSeconds: Long
) : MailProvider {

    private val mapper = jacksonObjectMapper()

    override fun sendText(to: String, subject: String, body: String, bcc: String?) {
        val payload = mapOf(
            "to" to to,
            "bcc" to bcc,
            "subject" to subject,
            "body" to body
        )
        val json = mapper.writeValueAsString(payload)

        val process = ProcessBuilder(pythonCmd, scriptPath)
            .redirectErrorStream(true)
            .start()

        process.outputStream.use { os ->
            os.write(json.toByteArray(StandardCharsets.UTF_8))
            os.flush()
        }

        val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        val output = process.inputStream.bufferedReader().readText()

        if (!finished) {
            process.destroyForcibly()
            throw IllegalStateException("Python mail timeout after ${timeoutSeconds}s")
        }
        if (process.exitValue() != 0) {
            throw IllegalStateException("Python mail failed (exit=${process.exitValue()}): $output")
        }
    }
}
