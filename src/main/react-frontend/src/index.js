import 'bootstrap/dist/css/bootstrap.min.css';
import React from 'react';
import ReactDOM from 'react-dom';
import { HashRouter, Routes, Route } from 'react-router-dom';

import './index.css';

import Home from './homepage/home';
import PdfViewer from './viewer/PdfViewer';

ReactDOM.render(
  <HashRouter>
    <div className="container">
      <Routes>
          <Route path="/" element={<Home/>}></Route>
          <Route path="/view" element={<PdfViewer/>}></Route>
      </Routes>
    </div>
  </HashRouter>,
  document.getElementById('root')
);
