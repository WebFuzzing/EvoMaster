package org.evomaster.core

import org.testcontainers.containers.GenericContainer

/**
 * See issue:
 * https://github.com/testcontainers/testcontainers-java/issues/318
 *
 * Created by arcuri82 on 01-Apr-19.
 */
class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)