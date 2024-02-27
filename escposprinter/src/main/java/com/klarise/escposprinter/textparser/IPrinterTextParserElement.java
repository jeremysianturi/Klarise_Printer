package com.klarise.escposprinter.textparser;

import com.klarise.escposprinter.EscPosPrinterCommands;
import com.klarise.escposprinter.exceptions.EscPosConnectionException;
import com.klarise.escposprinter.exceptions.EscPosEncodingException;

public interface IPrinterTextParserElement {
    int length() throws EscPosEncodingException;
    IPrinterTextParserElement print(EscPosPrinterCommands printerSocket) throws EscPosEncodingException, EscPosConnectionException;
}
