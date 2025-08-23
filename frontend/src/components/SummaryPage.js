import React, { useState, useEffect } from 'react';
import { Typography, Paper } from '@mui/material';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

const placeholderData = [
    { fund: 'Fund A', received: 4000, created: 2400, exceptions: 240 },
    { fund: 'Fund B', received: 3000, created: 1398, exceptions: 221 },
    { fund: 'Fund C', received: 2000, created: 9800, exceptions: 229 },
    { fund: 'Fund D', received: 2780, created: 3908, exceptions: 200 },
    { fund: 'Fund E', received: 1890, created: 4800, exceptions: 218 },
];

const SummaryPage = () => {
    const [summaryData, setSummaryData] = useState([]);

    useEffect(() => {
        // In a real application, you would fetch this data from an API.
        setSummaryData(placeholderData);
    }, []);

    return (
        <div>
            <Typography variant="h4" gutterBottom>
                Trade Summary
            </Typography>
            <Paper style={{ padding: '20px' }}>
                <ResponsiveContainer width="100%" height={400}>
                    <BarChart
                        data={summaryData}
                        margin={{
                            top: 20, right: 30, left: 20, bottom: 5,
                        }}
                    >
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="fund" />
                        <YAxis />
                        <Tooltip />
                        <Legend />
                        <Bar dataKey="received" fill="#8884d8" name="Trades Received" />
                        <Bar dataKey="created" fill="#82ca9d" name="Trades Created" />
                        <Bar dataKey="exceptions" fill="#ffc658" name="Exceptions" />
                    </BarChart>
                </ResponsiveContainer>
            </Paper>
        </div>
    );
};

export default SummaryPage;
