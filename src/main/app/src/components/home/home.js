import React, {Component} from 'react'

import Page from '../Page'
import Websocket from 'react-websocket';
import ReactTable from 'react-table';
import 'react-table/react-table.css';
import SymbolDetails from '../symbol-details';

class Home extends Component {

    constructor(props) {
        super(props);
        this.state = {
            symbols: [],
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
        setInterval(() => this.sendMessage('LOAD_SYMBOLS'), 1000);
    }

    handleData(data) {
        console.log(data);
        let result = JSON.parse(data);
        this.setState({symbols: result.symbols});
    }

    render() {
        const {symbols} = this.state;

        const columns = [
            {
                Header: 'Symbols',
                columns: [
                    {
                        Header: 'Symbol',
                        accessor: 'symbol'
                    },
                    {
                        Header: 'Name',
                        accessor: 'name'
                    },
                    {
                        Header: 'Price',
                        accessor: 'price'
                    },
                    {
                        Header: 'Volume',
                        accessor: 'volume'
                    },

                ]
            }
        ];
        return <Page header="Trade Monitor Dashboard">
            <ReactTable
                data={symbols}
                columns={columns}
                defaultPageSize={25}
                expanded={this.state.expanded}
                onExpandedChange={expanded => this.setState({expanded})}
                className="-striped -highlight"
                SubComponent={original => <SymbolDetails symbol={original.row.symbol}/>}
            />

            <Websocket url='ws://localhost:9000/trades' onOpen={this.onOpen}
                       onMessage={this.handleData}
                       reconnect={true} debug={true}
                       ref={Websocket => {
                           this.refWebSocket = Websocket;
                       }}/>
        </Page>;
    }
}

export default Home
