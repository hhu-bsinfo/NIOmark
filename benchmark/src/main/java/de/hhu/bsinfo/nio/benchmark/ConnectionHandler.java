package de.hhu.bsinfo.nio.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class ConnectionHandler extends Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionHandler.class);
    private static final String CONNECTION_MESSAGE = "CONNECTED";

    private final ConnectionReactor reactor;
    private final SocketChannel socketChannel;
    private final InetSocketAddress remoteAddress;
    private final ByteBuffer sendBuffer = ByteBuffer.allocateDirect(CONNECTION_MESSAGE.getBytes(StandardCharsets.UTF_8).length);
    private final ByteBuffer receiveBuffer = ByteBuffer.allocateDirect(CONNECTION_MESSAGE.getBytes(StandardCharsets.UTF_8).length);
    private final boolean outgoing;

    public ConnectionHandler(ConnectionReactor reactor, SocketChannel socketChannel, SelectionKey key, InetSocketAddress remoteAddress) {
        super(key);
        this.reactor = reactor;
        this.socketChannel = socketChannel;
        this.remoteAddress = remoteAddress;

        outgoing = true;
        sendBuffer.put(CONNECTION_MESSAGE.getBytes(StandardCharsets.UTF_8));
        sendBuffer.rewind();
    }

    public ConnectionHandler(ConnectionReactor reactor, SocketChannel socketChannel, SelectionKey key) {
        super(key);
        this.reactor = reactor;
        this.socketChannel = socketChannel;
        this.remoteAddress = null;

        outgoing = false;
        sendBuffer.put(CONNECTION_MESSAGE.getBytes(StandardCharsets.UTF_8));
        sendBuffer.rewind();
    }

    @Override
    protected void handle(final SelectionKey key) {
        if (key.isConnectable()) {
            handleConnect(key);
        } else if (key.isWritable()) {
            handleWrite(key);
        } else if (key.isReadable()) {
            handleRead(key);
        }
    }

    private void handleConnect(final SelectionKey key) {
        try {
            LOGGER.info("Finishing connection establishment to [{}]", socketChannel.getRemoteAddress());
        } catch (IOException e) {
            LOGGER.info("Handling connection event");
        }

        try {
            socketChannel.finishConnect();
        } catch (IOException e) {
            LOGGER.error("Failed to establish connection");
            key.cancel();
            reactor.addRemainingConnection(remoteAddress);
            return;
        }

        try {
            LOGGER.info("Established connection from [{}] to [{}]", socketChannel.getLocalAddress(), socketChannel.getRemoteAddress());
        } catch (IOException e) {
            LOGGER.info("Established connection");
        }

        key.interestOps(SelectionKey.OP_WRITE);
    }

    private void handleWrite(final SelectionKey key) {
        try {
            LOGGER.info("Sending connection message to [{}]", socketChannel.getRemoteAddress());
        } catch (IOException e) {
            LOGGER.info("Sending connection message");
        }

        try {
            final var written = socketChannel.write(sendBuffer);
            if (written == -1) {
                LOGGER.error("Failed to write connection message");
                key.cancel();
                reactor.addRemainingConnection(remoteAddress);
            }

            if (!sendBuffer.hasRemaining()) {
                key.interestOps(SelectionKey.OP_READ);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRead(final SelectionKey key) {
        try {
            LOGGER.info("Receiving connection message from [{}]", socketChannel.getRemoteAddress());
        } catch (IOException e) {
            LOGGER.info("Receiving connection message");
        }

        try {
            final var read = socketChannel.read(receiveBuffer);
            if (read == -1) {
                LOGGER.error("Failed to read connection message");
                key.cancel();
                reactor.addRemainingConnection(remoteAddress);
            }

            if (!sendBuffer.hasRemaining()) {
                finishConnection(key);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void finishConnection(final SelectionKey key) {
        if (outgoing) {
            key.interestOps(SelectionKey.OP_WRITE);
            // TODO: Attach benchmark handler
            key.attach(null);
            reactor.addEstablishedConnection(remoteAddress);
        } else {
            key.interestOps(SelectionKey.OP_READ);
            // TODO: Attach benchmark handler
            key.attach(null);
            reactor.addIncomingConnection();
        }

        try {
            close();
        } catch (IOException e) {
            LOGGER.error("Failed to close connection handler");
        }
    }

    @Override
    protected void close(final SelectionKey key) {
        try {
            LOGGER.info("Closing connection handler for connection from [{}] to [{}]", socketChannel.getLocalAddress(), socketChannel.getRemoteAddress());
        } catch (IOException e) {
            LOGGER.info("Closing connection handler");
        }
    }
}
