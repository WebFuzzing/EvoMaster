package com.foo.spring.rest.postgres.basic

import com.foo.spring.rest.postgres.SwaggerConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import springfox.documentation.swagger2.annotations.EnableSwagger2
import java.math.BigInteger
import java.util.regex.Pattern
import javax.persistence.EntityManager

/**
 * Created by arcuri82 on 21-Jun-19.
 */
@EnableSwagger2
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/basic"])
open class BasicApp : SwaggerConfiguration() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BasicApp::class.java, *args)
        }
    }

    @Autowired
    private lateinit var em: EntityManager


    @GetMapping
    open fun get(): ResponseEntity<Any> {

        // UUID, JSONB and XML are not supported by javax.persistence
        // The clause (p_at IS NULL or p_at IS NOT NULL) forces the p_at column to be selected as a gene
        val query = em.createNativeQuery("select id, textColumn, dateColumn, f_id, w_id, p_at, status from X where id>0 and (p_at IS NULL or p_at IS NOT NULL)")
        val res = query.resultList

        var status = 400
        if (res.isNotEmpty()) {
            val row = res[0] as Array<Any>
            val id = row[0] as BigInteger
            val textColumn = row[1] as String
            val dateColumn = row[2] as java.util.Date
            val f_id = row[3] as String
            val w_id = row[4] as String
            val p_atColumn = row[5] as Any?
            val statusColumn = row[6] as String


            var f_id_matches = false
            val likeJavaRegularExpressions = listOf("hi", ".*foo.*", ".*foo.*x.*", ".*bar.*", ".*bar.*y.*", ".*hello.*")
            for (likePatternStr in likeJavaRegularExpressions) {
                val likePattern = Pattern.compile(likePatternStr)
                val likeMatcher = likePattern.matcher(f_id)
                f_id_matches = f_id_matches || likeMatcher.find()
            }


            val similarToJavaRegEx = "/foo/../bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?"
            val pattern = Pattern.compile(similarToJavaRegEx)
            val matcher = pattern.matcher(w_id)
            val w_id_matches = matcher.find()

            val check_expr_is_satisfied = (p_atColumn != null) == (statusColumn == "B")

            if (w_id_matches && f_id_matches && check_expr_is_satisfied) {
                status = 200
            }
        }

        return ResponseEntity.status(status).build<Any>()
    }
}

