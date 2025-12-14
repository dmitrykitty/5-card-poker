package com.dnikitin.poker.common.protocol.commands;

import com.dnikitin.poker.common.protocol.Command;
import lombok.Getter;

/**
 * HELLO VERSION=<semver>
 */
@Getter
public class HelloCommand extends Command {
    private final String version;

    public HelloCommand(String version) {
        super(null, null, CommandType.HELLO);
        this.version = version;
    }
}
