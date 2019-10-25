import React, {Component} from 'react'

import Page from '../Page'
import Websocket from 'react-websocket';
import ReactTable from 'react-table';
import 'react-table/react-table.css';
import TickerDetails from '../ticker-details';

class Home extends Component {

    constructor(props) {
        super(props);
        this.state = {
            tickers: [],
            expanded: {}
        };

        this.sendMessage = this.sendMessage.bind(this);
        this.handleData = this.handleData.bind(this);
        this.onOpen = this.onOpen.bind(this);
    }

    sendMessage(message) {
        this.refWebSocket.sendMessage(message);
    }

    onOpen() {
        this.sendMessage('LOAD_TICKERS')
    }

    handleData(data) {
        console.log(data);
        let result = JSON.parse(data);
        this.setState({tickers: result.tickers});
    }

    render() {
        const {tickers} = this.state;

        const columns = [
            {
                Header: 'Tickers',
                columns: [
                    {
                        Header: 'Ticker Name',
                        accessor: 'ticker'
                    }
                ]
            }
        ];
        return <Page header="Real-time Trades">
            <ReactTable
                data={tickers}
                columns={columns}
                defaultPageSize={10}
                expanded={this.state.expanded}
                onExpandedChange={expanded => this.setState({expanded})}
                className="-striped -highlight"
                SubComponent={original => <TickerDetails ticker={original.row.ticker}/>}
            />

            <Websocket url='ws://localhost:9999/trades' onOpen={this.onOpen}
                       onMessage={this.handleData}
                       reconnect={true} debug={true}
                       ref={Websocket => {
                           this.refWebSocket = Websocket;
                       }}/>
        </Page>;
    }
}

export default Home
