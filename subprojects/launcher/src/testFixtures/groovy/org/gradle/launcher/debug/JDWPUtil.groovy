/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.launcher.debug

import com.sun.jdi.Bootstrap
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.VirtualMachine
import com.sun.jdi.VirtualMachineManager
import com.sun.jdi.connect.AttachingConnector
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A utility for communicating with a VM in debug mode using JDWP.  Currently
 * only supports the suspend and resume commands.
 */
class JDWPUtil implements TestRule {
    String host
    Integer port
    VirtualMachine vm
    private boolean isConnected

    JDWPUtil(Integer port) {
        this.port = port
    }

    JDWPUtil(String host, Integer port) {
        this.host = host
        this.port = port
    }

    @Override
    Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            void evaluate() throws Throwable {
                base.evaluate()
                close()
            }
        }
    }

    public JDWPUtil connect() {
        if (!isConnected) {
            VirtualMachineManager vmm = Bootstrap.virtualMachineManager()
            AttachingConnector connection = vmm.attachingConnectors().find { "dt_socket".equalsIgnoreCase(it.transport().name()) }
            def connectionArgs = connection.defaultArguments()
            connectionArgs.get("port").setValue(port as String)
            connectionArgs.get("hostname").setValue(host ?: "127.0.0.1")
            vm = connection.attach(connectionArgs)

            // Clean up any residual connection
            close()

            // If we get here, we have a successful connection and can send commands
            isConnected = true
        }
        return this
    }

    public void close() {
        if (vm != null) {
            try {
                vm.dispose()
            } catch (VMDisconnectedException e) {
                // This is ok - we're just trying to make sure all resources are released
            }
        }
        isConnected = false
    }

    public JDWPUtil resume() {
        println "Sending resume command..."
        vm.resume()
        return this
    }

    public JDWPUtil suspend() {
        println "Sending suspend command..."
        vm.suspend()
    }

    private void verifyConnected() {
        if (! isConnected) {
            throw new IllegalStateException("Not connected to debug VM - call connect() first")
        }
    }
}
