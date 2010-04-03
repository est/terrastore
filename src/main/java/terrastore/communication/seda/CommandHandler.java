package terrastore.communication.seda;

import terrastore.communication.protocol.Command;

/**
 * @author Sergio Bossa
 */
public interface CommandHandler<R> {

    public R handle(Command<R> command) throws Exception;
}