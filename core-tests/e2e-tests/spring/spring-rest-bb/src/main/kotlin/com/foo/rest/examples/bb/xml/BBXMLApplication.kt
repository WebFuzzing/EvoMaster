package com.foo.rest.examples.bb.xml

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.MediaType
import javax.xml.bind.annotation.*

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/bbxml"])
@RestController
open class BBXMLApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBXMLApplication::class.java, *args)
        }
    }

    /* ===================== ENDPOINTS ===================== */

    // 1. String -> XML
    @PostMapping(
        "/receive-string-respond-xml",
        consumes = ["text/plain"],
        produces = ["application/xml"]
    )
    fun stringToXml(@RequestBody body: String): ResponseEntity<Person> {
        if (body.isBlank())
            return ResponseEntity.status(400).build()

        return ResponseEntity.status(200)
            .body(Person(name = body, age = body.length))
    }

    // 2. XML -> String
    @PostMapping(
        "/receive-xml-respond-string",
        consumes = ["application/xml"],
        produces = ["text/plain"]
    )
    fun xmlToString(@RequestBody person: Person): ResponseEntity<String> {
        if (!isValid(person))
            return ResponseEntity
                .status(400).contentType(MediaType.TEXT_PLAIN).body("")

        return ResponseEntity.status(200).body("not ok")
    }

    // 3. Employee (2-level nesting)
    @PostMapping(
        "/employee",
        consumes = ["application/xml"],
        produces = ["text/plain"]
    )
    fun employee(@RequestBody employee: Employee): ResponseEntity<String> {
        if (!isValid(employee))
            return ResponseEntity.status(400)
                .contentType(MediaType.TEXT_PLAIN)
                .body("invalid input")

        return ResponseEntity.status(200)
            .body(
                if (employee.role == Role.ADMIN && employee.person.age > 30)
                    "admin"
                else
                    "not admin or too young"
            )
    }

    // 4. Company (3-level nesting)
    @PostMapping(
        "/company",
        consumes = ["application/xml"],
        produces = ["text/plain"]
    )
    fun company(@RequestBody company: Company): ResponseEntity<String> {
        if (!isValid(company))
            return ResponseEntity.status(400).body("")

        return ResponseEntity.status(200)
            .body(if (company.employees.isEmpty()) "small company" else "big company")
    }

    // 5. Department (recursive)
    @PostMapping(
        "/department",
        consumes = ["application/xml"],
        produces = ["text/plain"]
    )
    fun department(@RequestBody department: Department): ResponseEntity<String> {
        if (!isValid(department))
            return ResponseEntity.status(400)
                .contentType(MediaType.TEXT_PLAIN)
                .body("invalid input")

        return ResponseEntity.status(200)
            .body("department with ${department.employees.size + 1} employees")
    }

    // 6. Organization (3 lists)
    @PostMapping(
        "/organization",
        consumes = ["application/xml"],
        produces = ["text/plain"]
    )
    fun organization(@RequestBody organization: Organization): ResponseEntity<String> {
        if (!isValid(organization))
            return ResponseEntity.status(400)
                .contentType(MediaType.TEXT_PLAIN)
                .body("invalid input")

        return ResponseEntity.status(200)
            .body("organization with ${organization.people.size} people")
    }

    // 7. Project (attributes)
    @PostMapping(
        "/project",
        consumes = ["application/xml"],
        produces = ["text/plain"]
    )
    fun project(@RequestBody project: Project): ResponseEntity<String> {
        if (!isValid(project))
            return ResponseEntity.status(400).body("")

        var adults = 0
        for (m in project.members) {
            if (m.id.isNotBlank() && m.age >= 18)
                adults++
        }

        return ResponseEntity.status(200).body(
            if (adults > 0)
                "project ${project.code} has $adults adult members"
            else
                "project ${project.code} has only minors"
        )
    }

    // 8. Project list
    @PostMapping(
        "/projects",
        consumes = ["application/xml"],
        produces = ["text/plain"]
    )
    fun postProjects(@RequestBody list: ProjectList): ResponseEntity<String> {
        if (!isValid(list))
            return ResponseEntity.status(400).body("")

        var members = 0
        var hasCode = false

        for (p in list.projects) {
            if (p.code.isNotBlank())
                hasCode = true
            for (m in p.members) {
                if (m.id.isNotBlank())
                    members++
            }
        }

        return ResponseEntity.status(200)
            .body(
                if (hasCode && members > 0)
                    "valid projects with $members members"
                else
                    "invalid projects"
            )
    }

    // 9. Person with attributes
    @PostMapping(
        "/person-with-attr",
        consumes = ["application/xml"],
        produces = ["text/plain"]
    )
    fun personWithAttr(@RequestBody person: PersonWithAttr): ResponseEntity<String> {

        if (!isValid(person))
            return ResponseEntity.status(400).body("invalid person")

        return ResponseEntity.status(200)
            .body("person ${person.id} is valid")
    }

    /* ===================== SCHEMA-LIKE VALIDATION ===================== */

    private fun isValid(p: Person): Boolean =
        p.name.isNotBlank() && p.age >= 0

    private fun isValid(e: Employee): Boolean =
        isValid(e.person)

    private fun isValid(c: Company): Boolean =
        c.name.isNotBlank()

    private fun isValid(d: Department): Boolean =
        d.name.isNotBlank()

    private fun isValid(o: Organization): Boolean =
        o.name.isNotBlank()

    private fun isValid(p: PersonWithAttr): Boolean =
        p.id.isNotBlank() &&
                p.name.isNotBlank() &&
                p.age >= 0

    private fun isValid(p: Project): Boolean =
        p.code.isNotBlank()

    private fun isValid(pl: ProjectList): Boolean =
        true
}

/* ===================== MODELS (JAXB) ===================== */

@XmlRootElement(name = "person")
@XmlAccessorType(XmlAccessType.FIELD)
open class Person(
    var name: String = "",
    var age: Int = 0
)

@XmlRootElement(name = "employee")
@XmlAccessorType(XmlAccessType.FIELD)
open class Employee(
    var person: Person = Person(),
    var role: Role = Role.USER
)

@XmlRootElement(name = "company")
@XmlAccessorType(XmlAccessType.FIELD)
open class Company(
    var name: String = "",
    @field:XmlElement(name = "Person", namespace = "")
    var employees: MutableList<Person> = mutableListOf()
)

enum class Role { ADMIN, USER, GUEST }

@XmlRootElement(name = "department")
@XmlAccessorType(XmlAccessType.FIELD)
open class Department(
    var name: String = "",
    @field:XmlElement(name = "Employee", namespace = "")
    var employees: List<Employee> = emptyList(),
    @field:XmlElement(name = "Department", namespace = "")
    var subDepartments: MutableList<Department> = mutableListOf()
)

@XmlRootElement(name = "organization")
@XmlAccessorType(XmlAccessType.FIELD)
open class Organization(
    var name: String = "",
    @field:XmlElement(name = "Person", namespace = "")
    var people: MutableList<Person> = mutableListOf(),
    @field:XmlElement(name = "Employee", namespace = "")
    var employees: MutableList<Employee> = mutableListOf(),
    @field:XmlElement(name = "Company", namespace = "")
    var companies: MutableList<Company> = mutableListOf()
)

@XmlRootElement(name = "personWithAttr")
@XmlAccessorType(XmlAccessType.FIELD)
open class PersonWithAttr(
    @XmlAttribute(name = "id")
    var id: String = "",
    var name: String = "",
    var age: Int = 0
)

@XmlRootElement(name = "project")
@XmlAccessorType(XmlAccessType.FIELD)
open class Project(
    @XmlAttribute(name = "code")
    var code: String = "",
    @field:XmlElement(name = "PersonWithAttr", namespace = "")
    var members: MutableList<PersonWithAttr> = mutableListOf()
)

@XmlRootElement(name = "projectList")
@XmlAccessorType(XmlAccessType.FIELD)
open class ProjectList(
    @field:XmlElement(name = "Project", namespace = "")
    var projects: MutableList<Project> = mutableListOf()
)

