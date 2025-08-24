import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Typography, Paper, CircularProgress, Alert } from '@mui/material';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

const SummaryPage = () => {
    const [summaryData, setSummaryData] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchSummaryData = async () => {
            try {
                setLoading(true);
                const response = await axios.get('/api/summary/trades-by-fund', { withCredentials: true });
                if (response.data && response.data.success) {
                    setSummaryData(response.data.data);
                } else {
                    setError(response.data.message || 'Failed to fetch summary data.');
                }
            } catch (err) {
                setError('Failed to fetch summary data. Make sure the backend is running.');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };

        fetchSummaryData();
    }, []);

    if (loading) {
        return <CircularProgress />;
    }

    if (error) {
        return <Alert severity="error">{error}</Alert>;
    }

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
                        <XAxis dataKey="fundNumber" />
                        <YAxis />
                        <Tooltip />
                        <Legend />
                        <Bar dataKey="tradesReceived" fill="#8884d8" name="Trades Received" />
                        <Bar dataKey="tradesCreated" fill="#82ca9d" name="Trades Created" />
                        <Bar dataKey="exceptions" fill="#ffc658" name="Exceptions" />
                    </BarChart>
                </ResponsiveContainer>
            </Paper>
        </div>
    );
};

export default SummaryPage;
