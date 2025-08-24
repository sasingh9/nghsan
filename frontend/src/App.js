import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link as RouterLink } from 'react-router-dom';
import {
    AppBar,
    Box,
    CssBaseline,
    Drawer,
    List,
    ListItem,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Toolbar,
    Typography
} from '@mui/material';
import { BarChart as BarChartIcon, Inbox as InboxIcon, Mail as MailIcon, Assessment as AssessmentIcon, AccountBalanceWallet as AccountBalanceWalletIcon } from '@mui/icons-material';

import JsonDataViewer from './components/JsonDataViewer';
import TradeInquiry from './components/TradeInquiry';
import TradeExceptionInquiry from './components/TradeExceptionInquiry';
import SummaryPage from './components/SummaryPage';
import FundMaster from './components/FundMaster';

const drawerWidth = 240;

const navItems = [
    { text: 'Summary', path: '/', icon: <BarChartIcon /> },
    { text: 'Fund Master', path: '/fund-master', icon: <AccountBalanceWalletIcon /> },
    { text: 'JSON Data Viewer', path: '/json-data-viewer', icon: <AssessmentIcon /> },
    { text: 'Trade Inquiry', path: '/trade-inquiry', icon: <InboxIcon /> },
    { text: 'Trade Exceptions Inquiry', path: '/trade-exception-inquiry', icon: <MailIcon /> },
];

function App() {
    return (
        <Router>
            <Box sx={{ display: 'flex' }}>
                <CssBaseline />
                <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
                    <Toolbar>
                        <Typography variant="h6" noWrap component="div">
                            Trade Manager
                        </Typography>
                    </Toolbar>
                </AppBar>
                <Drawer
                    variant="permanent"
                    sx={{
                        width: drawerWidth,
                        flexShrink: 0,
                        [`& .MuiDrawer-paper`]: { width: drawerWidth, boxSizing: 'border-box' },
                    }}
                >
                    <Toolbar />
                    <Box sx={{ overflow: 'auto' }}>
                        <List>
                            {navItems.map((item) => (
                                <ListItem key={item.text} disablePadding>
                                    <ListItemButton component={RouterLink} to={item.path}>
                                        <ListItemIcon>
                                            {item.icon}
                                        </ListItemIcon>
                                        <ListItemText primary={item.text} />
                                    </ListItemButton>
                                </ListItem>
                            ))}
                        </List>
                    </Box>
                </Drawer>
                <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
                    <Toolbar />
                    <Routes>
                        <Route path="/" element={<SummaryPage />} />
                        <Route path="/fund-master" element={<FundMaster />} />
                        <Route path="/json-data-viewer" element={<JsonDataViewer />} />
                        <Route path="/trade-inquiry" element={<TradeInquiry />} />
                        <Route path="/trade-exception-inquiry" element={<TradeExceptionInquiry />} />
                    </Routes>
                </Box>
            </Box>
        </Router>
    );
}

export default App;
