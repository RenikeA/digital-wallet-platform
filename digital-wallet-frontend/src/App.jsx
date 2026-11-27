import { Routes, Route } from 'react-router-dom'
import Login from './pages/Login.jsx'

function Home() {
  return <h1>Home</h1>
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Login />} />
      <Route path="/home" element={<Home />} />
    </Routes>
  )
}