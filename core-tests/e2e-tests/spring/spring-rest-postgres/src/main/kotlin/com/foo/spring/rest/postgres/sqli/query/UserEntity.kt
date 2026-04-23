package com.foo.spring.rest.postgres.sqli.query

import javax.persistence.*


@Entity
@Table(name = "users")
open class UserEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    open var id: Long? = null,

    @Column(name = "username", unique = true, nullable = false)
    open var username: String? = null,

    @Column(name = "password", nullable = false)
    open var password: String? = null,
)
