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

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

/**
 * We use java.io.{@link Serializable} here for the sake of simplicity.
 * In production, Hazelcast Custom Serialization should be used.
 */
public class Trade implements IdentifiedDataSerializable {

    private String id;
    private long time;
    private String symbol;
    private int quantity;
    private int price; // in cents

    Trade() {
    }

    public Trade(String id, long time, String symbol, int quantity, int price) {
        this.id = id;
        this.time = time;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
    }

    public Trade(String line) {
        String[] args = line.split(" ");
        this.id = args[0];
        this.time = Long.parseLong(args[1]);
        this.symbol = args[2];
        this.quantity = Integer.parseInt(args[3]);
        this.price = Integer.parseInt(args[4]);
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
        return String.format("%s %d %s %d %d", id, time, symbol, quantity, price);
    }

    @Override
    public int getFactoryId() {
        return TradeSerializerHook.FACTORY_ID;
    }

    public String getTradeId() {
        return id;
    }

    @Override
    public int getId() {
        return TradeSerializerHook.TRADE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Trade trade = (Trade) o;
        return time == trade.time &&
                quantity == trade.quantity &&
                price == trade.price &&
                Objects.equals(id, trade.id) &&
                Objects.equals(symbol, trade.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, time, symbol, quantity, price);
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeUTF(id);
        out.writeLong(time);
        out.writeUTF(symbol);
        out.writeInt(quantity);
        out.writeInt(price);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        id = in.readUTF();
        time = in.readLong();
        symbol = in.readUTF();
        quantity = in.readInt();
        price = in.readInt();
    }
}