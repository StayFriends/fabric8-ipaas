/*
 *
 *  * Copyright 2005-2015 Red Hat, Inc.
 *  * Red Hat licenses this file to you under the Apache License, version
 *  * 2.0 (the "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  * implied.  See the License for the specific language governing
 *  * permissions and limitations under the License.
 *
 */
package io.fabric8.mq.protocol.openwire;

import io.fabric8.mq.protocol.ProtocolDecoder;
import org.apache.activemq.command.Command;
import org.apache.activemq.openwire.OpenWireFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ProtocolException;

/**
 * Implements protocol decoding for the Openwire protocol.
 */
class OpenwireProtocolDecoder extends ProtocolDecoder<Command> {

    private static final transient Logger LOG = LoggerFactory.getLogger(OpenwireProtocolDecoder.class);
    private final OpenwireProtocol protocol;
    public boolean trim = false;
    OpenWireFormat format = new OpenWireFormat(1);
    final Action<Command> read_action = new Action<Command>() {
        public Command apply() throws IOException {
            Buffer header = peekBytes(4);
            if (header == null) {
                return null;
            } else {
                final int length = header.getInt(0);
                if (length > protocol.maxFrameSize) {
                    throw new ProtocolException("Max frame size exceeded.");
                }
                nextDecodeAction = new Action<Command>() {
                    public Command apply() throws IOException {
                        Buffer frame = readBytes(4 + length);
                        if (frame == null) {
                            return null;
                        } else {
                            ByteArrayInputStream dataIn = new ByteArrayInputStream(frame.getBytes());
                            DataInputStream dataInputStream = new DataInputStream(dataIn);
                            Command command = (Command) format.unmarshal(dataInputStream);
                            nextDecodeAction = read_action;
                            return command;
                        }
                    }
                };
                return nextDecodeAction.apply();
            }
        }
    };

    public OpenwireProtocolDecoder(OpenwireProtocol protocol) {
        this.protocol = protocol;
    }

    @Override
    protected Action<Command> initialDecodeAction() {
        return read_action;
    }

}
