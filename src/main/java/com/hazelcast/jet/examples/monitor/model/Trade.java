/*
 * Copyright (c) 2008-2019, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jet.examples.monitor.model;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

import java.io.IOException;
import java.io.Serializable;

/**
 * We use java.io.{@link Serializable} here for the sake of simplicity.
 * In production, Hazelcast Custom Serialization should be used.
 */
public class Trade implements IdentifiedDataSerializable {

    private long time;
    private String symbol;
    private int quantity;
    private int price; // in cents

    Trade() {
    }

    public Trade(long time, String symbol, int quantity, int price) {
        this.time = time;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
    }

    public Trade(String line) {
        String[] args = line.split(" ");
        this.time = Long.parseLong(args[0]);
        this.symbol = args[1];
        this.quantity = Integer.parseInt(args[2]);
        this.price = Integer.parseInt(args[3]);
    }

    /**
     * Timestamp for the trade in UNIX time
     */
    public long getTime() {
        return time;
    }

    /**
     * The symbol
     */
    public String getSymbol() {
        return symbol;
    }

    public int getQuantity() {
        return quantity;
    }

    /**
     * The price in cents
     */
    public int getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return String.format("%d %s %d %d", time, symbol, quantity, price);
    }

    @Override
    public int getFactoryId() {
        return TradeSerializerHook.FACTORY_ID;
    }

    @Override
    public int getId() {
        return TradeSerializerHook.TRADE;
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeLong(time);
        out.writeUTF(symbol);
        out.writeInt(quantity);
        out.writeInt(price);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        time = in.readLong();
        symbol = in.readUTF();
        quantity = in.readInt();
        price = in.readInt();
    }
}