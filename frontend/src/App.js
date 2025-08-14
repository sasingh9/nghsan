import React, { useState } from 'react';
import axios from 'axios';
import './App.css';

function App() {
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const fetchData = async () => {
        setLoading(true);
        setError('');
        setData([]);

        // Basic validation
        if (!startDate || !endDate) {
            setError('Please select both a start and end date.');
            setLoading(false);
            return;
        }

        try {
            const params = {
                startDate: new Date(startDate).toISOString(),
                endDate: new Date(endDate).toISOString(),
            };
            const response = await axios.get('/api/data', { params });
            setData(response.data);
        } catch (err) {
            setError('Failed to fetch data. Make sure the backend is running.');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="App">
            <header className="App-header">
                <h1>JSON Data Viewer</h1>
            </header>
            <main>
                <div className="filter-container">
                    <div className="date-picker">
                        <label>Start Date:</label>
                        <input
                            type="datetime-local"
                            value={startDate}
                            onChange={(e) => setStartDate(e.target.value)}
                        />
                    </div>
                    <div className="date-picker">
                        <label>End Date:</label>
                        <input
                            type="datetime-local"
                            value={endDate}
                            onChange={(e) => setEndDate(e.target.value)}
                        />
                    </div>
                    <button onClick={fetchData} disabled={loading}>
                        {loading ? 'Loading...' : 'Fetch Data'}
                    </button>
                </div>
                {error && <p className="error-message">{error}</p>}
                <div className="data-container">
                    {data.length > 0 ? (
                        data.map((item) => (
                            <div key={item.id} className="data-item">
                                <h3>Record ID: {item.id}</h3>
                                <p><strong>Message Key:</strong> {item.messageKey}</p>
                                <p><strong>Created At:</strong> {new Date(item.createdAt).toLocaleString()}</p>
                                <pre>{JSON.stringify(JSON.parse(item.jsonData), null, 2)}</pre>
                            </div>
                        ))
                    ) : (
                        <p>{!loading && 'No data to display. Adjust the filter and click "Fetch Data".'}</p>
                    )}
                </div>
            </main>
        </div>
    );
}

export default App;
