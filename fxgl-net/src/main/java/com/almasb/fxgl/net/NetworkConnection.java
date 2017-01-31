/*
 * The MIT License (MIT)
 *
 * FXGL - JavaFX Game Library
 *
 * Copyright (c) 2015-2017 AlmasB (almaslvl@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.almasb.fxgl.net;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a communication between two machines over network.
 *
 * @author Almas Baimagambetov (AlmasB) (almaslvl@gmail.com)
 */
public abstract class NetworkConnection {

    private static final Logger log = LogManager.getLogger(NetworkConnection.class);

    protected Map<Class<?>, DataParser<? super Serializable>> parsers = new HashMap<>();

    public Map<Class<?>, DataParser<? super Serializable>> getParsers() {
        return parsers;
    }

    public void setParsers(Map<Class<?>, DataParser<? super Serializable>> parsers) {
        this.parsers = parsers;
    }

    private ReadOnlyBooleanWrapper connectionActive = new ReadOnlyBooleanWrapper(false);

    public final ReadOnlyBooleanProperty connectionActiveProperty() {
        return connectionActive.getReadOnlyProperty();
    }

    public boolean isConnectionActive() {
        return connectionActive.get();
    }

    private Runnable onConnectionOpen = null;

    public void setOnConnectionOpen(Runnable onConnectionOpen) {
        this.onConnectionOpen = onConnectionOpen;
    }

    protected void onConnectionOpen() {
        if (onConnectionOpen != null)
            onConnectionOpen.run();

        connectionActive.set(true);
    }

    private Runnable onConnectionClosed = null;

    public void setOnConnectionClosed(Runnable onConnectionClosed) {
        this.onConnectionClosed = onConnectionClosed;
    }

    protected void onConnectionClosed() {
        if (onConnectionClosed != null)
            onConnectionClosed.run();

        connectionActive.set(false);
    }

    /**
     * Send a message (hint) that this end of connection is about
     * to close
     */
    protected void sendClosingMessage() {
        try {
            send(ConnectionMessage.CLOSING, NetworkProtocol.TCP);
        } catch (Exception e) {
            log.warn("TCP already disconnected or error: " + e.getMessage());
        }

        try {
            send(ConnectionMessage.CLOSING, NetworkProtocol.UDP);
        } catch (Exception e) {
            log.warn("UDP already disconnected or error: " + e.getMessage());
        }
    }

    /**
     * Register a parser for specified class. The parser
     * will be called back when an instance of the class
     * arrives from the other end of connection
     *
     * @param cl data structure class
     * @param parser the data parser
     */
    @SuppressWarnings("unchecked")
    public <T extends Serializable> void addParser(Class<T> cl, DataParser<T> parser) {
        parsers.put(cl, (DataParser<? super Serializable>) parser);
    }

    /**
     * Send data to the machine at the other end using UDP protocol.
     *
     * @param data the data object
     * @throws Exception
     */
    public void send(Serializable data) throws Exception {
        send(data, NetworkProtocol.UDP);
    }

    /**
     * Send data to the machine at the other end using specified protocol
     *
     * @param data the data object
     * @param protocol the protocol to use
     * @throws Exception
     */
    public void send(Serializable data, NetworkProtocol protocol) throws Exception {
        if (protocol == NetworkProtocol.TCP)
            sendTCP(data);
        else
            sendUDP(data);
    }

    protected abstract void sendUDP(Serializable data) throws Exception;

    protected abstract void sendTCP(Serializable data) throws Exception;

    public abstract void close();

    protected static byte[] toByteArray(Serializable data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutput oo = new ObjectOutputStream(baos)) {
            oo.writeObject(data);
        }

        return baos.toByteArray();
    }
}
