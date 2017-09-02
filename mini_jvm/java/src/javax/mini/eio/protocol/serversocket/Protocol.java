/*
 * @(#)Socket.java	1.15 02/10/14 @(#)
 *
 * Copyright (c) 1999-2002 Sun Microsystems, Inc.  All rights reserved.
 * PROPRIETARY/CONFIDENTIAL
 * Use is subject to license terms.
 */
package javax.mini.eio.protocol.serversocket;

import com.sun.cldc.io.ConnectionBaseInterface;
import java.io.IOException;
import java.io.InterruptedIOException;
import javax.cldc.io.Connection;
import javax.cldc.io.Connector;

import javax.mini.eio.*;

/*
 * Note: Since this class references the TCP socket protocol class that
 * extends the NetworkConnectionBaseClass. The native networking will be
 * initialized when this class loads if needed without extending
 * NetworkConnectionBase.
 */
/**
 * StreamConnectionNotifier to the TCP Server Socket API.
 *
 * @author Nik Shaylor
 * @version 1.0 10/08/99
 */
public class Protocol implements ConnectionBaseInterface,ServerSocket {

    /**
     * Socket object used by native code, for now must be the first field.
     */
    private int handle;

    /**
     * Flag to indicate connection is currently open.
     */
    boolean connectionOpen = false;

    int port;

    String ip;
    @Override
    public Connection openPrim(String name, int mode, boolean timeouts) throws IOException {
        if (!name.startsWith("//")) {
            throw new IOException( /* #ifdef VERBOSE_EXCEPTIONS */ /// skipped                       "bad socket connection name: " + name
                    /* #endif */);
        }
        int i = name.indexOf(':');
        if (i < 0) {
            throw new IOException( /* #ifdef VERBOSE_EXCEPTIONS */ /// skipped                       "bad socket connection name: port missing"
                    /* #endif */);
        }
        String hostname = name.substring(2, i);
        int port;
        try {
            port = Integer.parseInt(name.substring(i + 1));
        } catch (NumberFormatException e) {
            throw new IOException( /* #ifdef VERBOSE_EXCEPTIONS */ /// skipped                       "bad socket connection name: bad port"
                    /* #endif */);
        }
        // cstring is always NUL terminated (note the extra byte allocated).
        // This avoids awkward char array manipulation in C code.
        byte cstring[] = new byte[hostname.length() + 1];
        for (int n = 0; n < hostname.length(); n++) {
            cstring[n] = (byte) (hostname.charAt(n));
        }
        if ((this.handle = open0(cstring, port)) < 0) {
            int errorCode = this.handle & 0x7fffffff;
            throw new IOException( /* #ifdef VERBOSE_EXCEPTIONS */ /// skipped                       "connection failed: error = " + errorCode
                    /* #endif */);
        }
        return this;
    }
    /**
     * Opens a port to listen on.
     *
     * @param port TCP to listen on
     *
     * @exception IOException if some other kind of I/O error occurs
     */
//    public void open(String ip, int port) throws IOException {
//        this.ip = ip;
//        this.port = port;
//        this.handle =open0(ip.getBytes(), port > 0 ? port : 0);
//        connectionOpen = true;
//
//        port = getLocalPort();
//
//        registerCleanup(this.handle);
//    }

    /**
     * Checks if the connection is open.
     *
     * @exception IOException is thrown, if the stream is not open
     */
    void ensureOpen() throws IOException {
        if (!connectionOpen) {
            throw new IOException("Connection closed");
        }
    }

    /**
     * Opens a native socket and put its handle in the handle field.
     * <p>
     * Called by socket Protocol class after it parses a given URL and finds no
     * host.
     *
     * @param port TCP port to listen for connections on
     * @param storage name of current suite storage
     *
     * @exception IOException if some other kind of I/O error occurs or if
     * reserved by another suite
     */
    public native int open0(byte[] ip, int port) throws IOException;

    /**
     * Returns a connection that represents a server side socket connection.
     * <p>
     * Polling the native code is done here to allow for simple asynchronous
     * native code to be written. Not all implementations work this way (they
     * block in the native code) but the same Java code works for both.
     *
     * @return a socket to communicate with a client.
     *
     * @exception IOException if an I/O error occurs when creating the input
     * stream
     */
    synchronized public javax.mini.eio.Socket accept()
            throws IOException {

        javax.mini.eio.protocol.socket.Protocol con;

        ensureOpen();

        while (true) {
            int clt_handle = accept0(this.handle);
            if (clt_handle >= 0) {
                con = new javax.mini.eio.protocol.socket.Protocol();
                con.open(clt_handle, Connector.READ_WRITE);
                break;
            }

            /* Wait a while for I/O to become ready */
            //GeneralBase.iowait(); 
        }

        return con;
    }

    /**
     * Gets the local address to which the socket is bound.
     *
     * <P>
     * The host address(IP number) that can be used to connect to this end of
     * the socket connection from an external system. Since IP addresses may be
     * dynamically assigned, a remote application will need to be robust in the
     * face of IP number reasssignment.</P>
     * <P>
     * The local hostname (if available) can be accessed from
     * <code> System.getProperty("microedition.hostname")</code>
     * </P>
     *
     * @return the local address to which the socket is bound
     * @exception IOException if the connection was closed
     * @see ServerSocketConnection
     */
    public String getLocalAddress() throws IOException {
        ensureOpen();
        return ip + ":" + port;
    }

    /**
     * Returns the local port to which this socket is bound.
     *
     * @return the local port number to which this socket is connected
     * @exception IOException if the connection was closed
     * @see ServerSocketConnection
     */
    public int getLocalPort() throws IOException {
        ensureOpen();
        return port;
    }

    /**
     * Closes the connection, accesses the handle field.
     *
     * @exception IOException if an I/O error occurs when closing the connection
     */
    public void close() throws IOException {
        if (connectionOpen) {
            close0(handle);
            connectionOpen = false;
        }
    }

    private native int listen0(int handle) throws IOException;

    /**
     * Accepts a TCP connection socket handle to a client, accesses the handle
     * field.
     *
     * @return TCP connection socket handle
     *
     * @exception IOException If some other kind of I/O error occurs.
     */
    private native int accept0(int handle) throws IOException;

    /**
     * Closes the connection, accesses the handle field.
     *
     * @exception IOException if an I/O error occurs when closing the connection
     */
    public native void close0(int handle) throws IOException;

    /**
     * Registers with the native cleanup code, accesses the handle field.
     */
    private native void registerCleanup(int handle);

    /**
     * Native finalizer
     */
    private native void finalize(int handle);

    @Override
    public void listen() throws IOException {
        listen0(handle);
    }


}