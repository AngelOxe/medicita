
  import { Navigate, Route, Routes } from 'react-router-dom'
import UiDemo from './pages/UiDemo'

export default function App() {
  return (
    <Routes>
      <Route path="/ui-demo" element={<UiDemo />} />
      <Route path="*" element={<Navigate to="/ui-demo" replace />} />
    </Routes>
  )
}
