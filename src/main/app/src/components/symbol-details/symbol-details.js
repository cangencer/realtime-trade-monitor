import React, {Component, Fragment} from 'react'

import Websocket from 'react-websocket';
import ReactTable from 'react-table';
import 'react-table/react-table.css';
import Pagination from '../Pagination';

class SymbolDetails extends Component {

    constructor(props) {
        super(props);
        this.state = {
            symbol: []
        };

        this.sendMessage = this.sendMessage.bind(this);
        this.handleData = this.handleData.bind(this);
        this.onOpen = this.onOpen.bind(this);
        this.renderPagination = this.renderPagination.bind(this);
    }

    sendMessage(message) {
        this.refWebSocket.sendMessage(message);
    }

    onOpen() {
        this.sendMessage('DRILL_SYMBOL ' + this.props.symbol);
    }

    handleData(data) {
        let result = JSON.parse(data);
        this.setState({symbol: this.state.symbol.concat(result.data)});
    }

    renderPagination(paginationProps) {
        return <Pagination {...paginationProps} extraData={<span>{this.props.symbol} has {this.state.symbol.length} records</span>}/>
    }

    render() {
        const {symbol} = this.state;

        const columns = [
            {
                Header: 'ID',
                accessor: 'id'
            },
            {
                Header: 'Time',
                accessor: 'time'
            },
            {
                Header: 'Symbol',
                accessor: 'symbol'
            },
            {
                Header: 'Quantity',
                accessor: 'quantity'
            },
            {
                Header: 'Price',
                accessor: 'price'
            }
        ];

        return <Fragment>
        <Websocket url='ws://localhost:9000/trades' onOpen={this.onOpen}
        onMessage={this.handleData}
        reconnect={true} debug={true}
        ref={Websocket => {
            this.refWebSocket = Websocket;
        }}/>
        <ReactTable
        data={symbol}
        columns={columns}
        defaultPageSize={10}
        expanded={this.state.expanded}
        onExpandedChange={expanded => this.setState({expanded})}
        className="Table-subtable"
        PaginationComponent={this.renderPagination}
        />
        </Fragment>
    }
}

export default SymbolDetails;
