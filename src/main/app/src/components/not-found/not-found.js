import React    from 'react'

import { Link } from 'react-router-dom'

import Page     from '../Page'

const NotFound = () => (
    <Page header="Not Found">
        <h2>Page not found</h2>
        <span><Link className="App-link" to="/">Home</Link></span>
    </Page>
)

export default NotFound
