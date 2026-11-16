
  import { Navigate, Route, Routes } from 'react-router-dom'
import UiDemo from './pages/UiDemo'
import Forbidden from './pages/Forbidden'

export default function App() {
  return (
    <Routes>
      <Route path="/ui-demo" element={<UiDemo />} />
      <Route path="/403" element={<Forbidden />} />
      <Route path="*" element={<Navigate to="/ui-demo" replace />} />
    </Routes>
  )
}
