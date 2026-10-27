import { NavLink, Outlet } from 'react-router';
import './admin.css';

function AdminLayout() {
    return <div className="admin-layout">
        <aside className="admin-sidebar">
            <h2>관리자 메뉴</h2>
            <nav aria-label="관리자 메뉴">
                <NavLink to="/admin/categories">카테고리</NavLink>
            </nav>
        </aside>
        <main className="admin-content"><Outlet /></main>
    </div>;
}

export default AdminLayout;
