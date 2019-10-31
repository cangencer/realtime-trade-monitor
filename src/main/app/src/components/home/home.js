import React, {Component} from 'react'

import Page from '../Page'
import Websocket from 'react-websocket';
import ReactTable from 'react-table';
import 'react-table/react-table.css';
import SymbolDetails from '../symbol-details';
import '../../Table.css';

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
        this.once = true
    }

    sendMessage(message) {
        this.refWebSocket.sendMessage(message);
    }

    onOpen() {
        setInterval(() => this.sendMessage('LOAD_SYMBOLS'), 1000);
    }

    handleData(data) {
        let result = JSON.parse(data);
        for (let i = 0; i < result.symbols.length; i++) {
            let oldPrice;
            if (typeof this.state.symbols[i] === 'undefined') {
                oldPrice = 0;
            } else {
                oldPrice = this.state.symbols[i].price;
            }
            result.symbols[i]["oldPrice"] = oldPrice;
        }
        this.setState({symbols: result.symbols});
    }

    render() {
        const {symbols} = this.state;

        const columns = [
            {
                Header: 'Symbol',
                accessor: 'symbol',
                Cell: ({value}) => <span className='Table-highlightValue'>{value}</span>,
            },
            {
                Header: 'Name',
                accessor: 'name'
            },
            {
                Header: 'Price',
                accessor: 'price',
                Cell: ({value, columnProps: {className}}) => <span className={`Table-highlightValue Table-price ${className}`}>{(value/100).toLocaleString("en-US", {style:"currency", currency:"USD"})}</span>,
                getProps: (state, ri, column) => {
                    if (!ri){
                        return {};
                    }
                    // console.log(ri.row);
                    const changeUp = ri.row.price > ri.row._original.oldPrice;
                    const changeDown = ri.row.price < ri.row._original.oldPrice;
                    const className =
                    changeUp ? 'Table-changeUp' : (changeDown ? 'Table-changeDown' : '')

                    return {
                        className,
                    };
                }
            },
            {
                Header: 'Volume',
                accessor: 'volume'
            },
        ];
        return <Page header="Trade Monitor Dashboard">
        <ReactTable
        className="Table-main"
        data={symbols}
        columns={columns}
        defaultPageSize={25}
        expanded={this.state.expanded}
        onExpandedChange={expanded => this.setState({expanded})}
        getTrProps={(state, rowInfo) => ({
            className: rowInfo && state.expanded[rowInfo.index] ? 'Table-expanded' : ''}
        )}
        getTdProps={(state, rowInfo, column, instance) => {
            return {
                onClick: (e, handleOriginal) => {
                    const {index} = rowInfo;
                    this.setState(prevState => ({
                        expanded: {
                            ...prevState.expanded,
                            [index]: !prevState.expanded[index],
                        }
                    }))
                    // IMPORTANT! React-Table uses onClick internally to trigger
                    // events like expanding SubComponents and pivots.
                    // By default a custom 'onClick' handler will override this functionality.
                    // If you want to fire the original onClick handler, call the
                    // 'handleOriginal' function.
                    // if (handleOriginal) {
                    //   handleOriginal()
                    // }
                }
            }
        }}
        //   NoDataComponent={}
        SubComponent={original => <SymbolDetails
            symbol={original.row.symbol}/>}
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
