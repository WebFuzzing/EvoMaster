package com.foo.rest.examples.spring.openapi.v3.xml

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAnyElement
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlRootElement



@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/xml"])
@RestController
open class XMLApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(XMLApplication::class.java, *args)
        }
    }

    /* ===================== ENDPOINTS ===================== */

    @PostMapping(
        "/receive-string-respond-xml",
        consumes = ["text/plain"],
        produces = ["application/xml"]
    )
    fun stringToXml(@RequestBody body: String): ResponseEntity<Person> {

        if (body.isBlank() || body.any {it.isDigit()})
            return ResponseEntity.status(400).build()

        return ResponseEntity.status(200)
            .body(Person(name = body, age = body.length))
    }

    @PostMapping(
        "/receive-xml-respond-string",
        consumes = ["application/xml"],
        produces = ["text/plain"]
    )
    fun xmlToString(@RequestBody person: Person): ResponseEntity<String> {

        if (!isValid(person))
            return ResponseEntity.status(400).build()

        return ResponseEntity.status(200).body("not ok")
    }

    @PostMapping(
        "/employee",
        consumes = ["application/xml"],
        produces = ["text/plain"]
    )
    fun employee(@RequestBody employee: Employee): ResponseEntity<String> {

        if (!isValid(employee))
            return ResponseEntity.status(400).build()

        return ResponseEntity.status(200)
            .body(
                if (employee.role == Role.ADMIN && employee.person.age > 30)
                    "admin"
                else
                    "not admin or too young"
            )
    }

    @PostMapping(
        "/company",
        consumes = ["application/xml"],
        produces = ["text/plain"]
    )
    fun company(@RequestBody company: Company): ResponseEntity<String> {

        if (!isValid(company))
            return ResponseEntity.status(400).build()

        return ResponseEntity.status(200)
            .body(if (company.employees.isEmpty()) "small company" else "big company")
    }

    @PostMapping(
        "/department",
        consumes = ["application/xml"],
        produces = ["text/plain"]
    )
    fun department(@RequestBody department: Department): ResponseEntity<String> {

        if (!isValid(department))
            return ResponseEntity.status(400).build()

        return ResponseEntity.status(200)
            .body("department with ${department.employees.size + 1} employees")
    }

    @PostMapping(
        "/organization",
        consumes = ["application/xml"],
        produces = ["text/plain"]
    )
    fun organization(@RequestBody organization: Organization): ResponseEntity<String> {

        if (!isValid(organization))
            return ResponseEntity.status(400).build()

        return ResponseEntity.status(200)
            .body("organization with ${organization.people.size} people")
    }

    @PostMapping(
        "/project",
        consumes = ["application/xml"],
        produces = ["text/plain"]
    )
    fun project(@RequestBody project: Project): ResponseEntity<String> {

        if (!isValid(project))
            return ResponseEntity.status(400).build()

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

    @PostMapping(
        "/projects",
        consumes = ["application/xml"],
        produces = ["text/plain"]
    )
    fun postProjects(@RequestBody list: ProjectList): ResponseEntity<String> {

        if (!isValid(list))
            return ResponseEntity.status(400).build()

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

    @PostMapping(
        "/person-with-attr",
        consumes = ["application/xml"],
        produces = ["text/plain"]
    )
    fun personWithAttr(@RequestBody person: PersonWithAttr): ResponseEntity<String> {

        if (!isValid(person))
            return ResponseEntity.status(400).build()

        return if (person.age >= 65)
            ResponseEntity.status(200).body("senior ${person.id}")
        else
            ResponseEntity.status(200).body("adult ${person.id}")
    }

    /* ===================== WHITE-BOX VALIDATION ===================== */

    private fun isValid(p: Person): Boolean =
        p.name.isNotBlank() &&
                p.name.length <= 20 &&
                p.age in 0..120 &&
                !(p.name == "admin" && p.age < 18)

    private fun isValid(e: Employee): Boolean =
        isValid(e.person) &&
                !(e.role == Role.ADMIN && e.person.age < 21)

    private fun isValid(c: Company): Boolean =
        c.name.isNotBlank() &&
                c.name.length >= 3 &&
                c.employees.size <= 50

    private fun isValid(d: Department): Boolean =
        d.name.isNotBlank() &&
                d.name != "root" &&
                d.employees.size <= 10

    private fun isValid(o: Organization): Boolean =
        o.name.isNotBlank() &&
                (o.people.size + o.employees.size + o.companies.size) <= 100 &&
                !(o.companies.isNotEmpty() && o.people.isEmpty())

    private fun isValid(p: PersonWithAttr): Boolean =
        p.id.isNotBlank() &&
                p.id.matches(Regex("[A-Z]{2}[0-9]{2}")) &&
                p.name.isNotBlank() &&
                p.age in 0..150

    private fun isValid(p: Project): Boolean =
                p.code.length >= 3 &&
                p.members.size >= 0

    private fun isValid(pl: ProjectList): Boolean =
        pl.projects.isNotEmpty() &&
                pl.projects.size <= 5 &&
                pl.projects.any { it.code.isNotBlank() }
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
    var employees: MutableList<Employee> = mutableListOf(),
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

@XmlRootElement(name = "PersonWithAttr")
@XmlAccessorType(XmlAccessType.FIELD)
open class PersonWithAttr(
    @XmlAttribute(name = "id")
    var id: String = "",
    var name: String = "",
    var age: Int = 0
)

@XmlRootElement(name = "Project")
@XmlAccessorType(XmlAccessType.FIELD)
open class Project(
    @XmlAttribute(name = "code")
    var code: String = "",
    @field:XmlElementWrapper(name = "members")
    @field:XmlElement(name = "PersonWithAttr", namespace = "")
    var members: MutableList<PersonWithAttr> = mutableListOf()
)

@XmlRootElement(name = "projectList")
@XmlAccessorType(XmlAccessType.FIELD)
open class ProjectList(
    @field:XmlElementWrapper(name = "projects")
    @field:XmlElement(name = "Project", namespace = "")
    var projects: MutableList<Project> = mutableListOf()
)