package de.hhu.bsinfo.nio.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

class Acceptor extends Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Acceptor.class);

    private final ConnectionReactor reactor;
    private final ServerSocketChannel serverSocketChannel;

    public Acceptor(final ConnectionReactor reactor, final ServerSocketChannel serverSocketChannel, final SelectionKey key) {
        super(key);
        this.reactor = reactor;
        this.serverSocketChannel = serverSocketChannel;
    }

    @Override
    protected void handle(final SelectionKey key) {
        if (key.isAcceptable()) {
            LOGGER.info("Handling incoming connection request");
            SocketChannel socketChannel;

            try {
                socketChannel = serverSocketChannel.accept();
                socketChannel.configureBlocking(false);
                final var socketKey = socketChannel.register(key.selector(), SelectionKey.OP_WRITE);
                final var handler = new ConnectionHandler(reactor, socketChannel, socketKey);
                socketKey.attach(handler);
            } catch (IOException e) {
                LOGGER.error("Failed to accept connection request", e);
                return;
            }

            try {
                LOGGER.info("Accepted connection request from [{}]", socketChannel.getRemoteAddress());
            } catch (IOException e) {
                LOGGER.info("Accepted connection request");
            }
        }
    }

    @Override
    protected void close(final SelectionKey key) throws IOException {
        LOGGER.info("Closing acceptor");
        key.cancel();
        serverSocketChannel.close();
    }

}
