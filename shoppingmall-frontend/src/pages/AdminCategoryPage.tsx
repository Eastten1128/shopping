import { useEffect, useState, type FormEvent } from 'react';
import axios from 'axios';
import { createCategory, deleteCategory, getAdminCategories, updateCategory } from '../api/categoryApi';
import type { AdminCategory } from '../types/category';
import DeleteCategoryDialog from '../components/DeleteCategoryDialog';

const errorMessage = (error: unknown, fallback: string) => axios.isAxiosError<{ message?: string }>(error)
    ? error.response?.data?.message ?? fallback : fallback;

interface EditorProps {
    category: AdminCategory | null;
    onSaved: () => void;
    onCancel: () => void;
}

function CategoryEditor({ category, onSaved, onCancel }: EditorProps) {
    const [name, setName] = useState(category?.categoryName ?? '');
    const [pending, setPending] = useState(false);
    const [error, setError] = useState('');
    const submit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (pending) return;
        setError('');
        if (!name.trim() || name.trim().length > 255) {
            setError('카테고리명은 공백을 제외해 1~255자로 입력해주세요.');
            return;
        }
        setPending(true);
        try {
            if (category) await updateCategory(category.categoryId, name.trim());
            else await createCategory(name.trim());
            onSaved();
        } catch (failure) {
            setError(errorMessage(failure, '카테고리 저장에 실패했습니다. 다시 시도해주세요.'));
        } finally { setPending(false); }
    };
    return <section className="admin-editor" aria-labelledby="category-editor-title">
        <h2 id="category-editor-title">{category ? '카테고리 수정' : '카테고리 생성'}</h2>
        {category?.deleted && <p>삭제된 카테고리입니다. 이름을 수정해도 삭제 상태는 유지됩니다.</p>}
        <form onSubmit={event => void submit(event)}>
            <label htmlFor="category-name">카테고리명</label>{' '}
            <input id="category-name" value={name} required maxLength={255} disabled={pending}
                onChange={event => setName(event.target.value)} />
            {error && <p role="alert">{error}</p>}
            <div className="admin-editor-actions">
                <button disabled={pending} type="submit">{pending ? '저장 중...' : '확인'}</button>
                <button disabled={pending} type="button" onClick={onCancel}>취소</button>
            </div>
        </form>
    </section>;
}

function AdminCategoryPage() {
    const [includeDeleted, setIncludeDeleted] = useState(false);
    const [reload, setReload] = useState(0);
    const key = `${includeDeleted}:${reload}`;
    const [listing, setListing] = useState<{ key: string; items: AdminCategory[]; error: string } | null>(null);
    const [editor, setEditor] = useState<{ category: AdminCategory | null } | null>(null);
    const [selected, setSelected] = useState<AdminCategory | null>(null);
    const [deleting, setDeleting] = useState(false);
    const [deleteError, setDeleteError] = useState('');
    const [message, setMessage] = useState('');
    const loading = listing?.key !== key;
    const busy = editor !== null || selected !== null;

    useEffect(() => {
        let active = true;
        getAdminCategories(includeDeleted).then(items => {
            if (active) setListing({ key, items: [...items].sort((a, b) => a.categoryId - b.categoryId), error: '' });
        }).catch(failure => {
            if (active) setListing({ key, items: [], error: errorMessage(failure, '카테고리 목록을 불러오지 못했습니다.') });
        });
        return () => { active = false; };
    }, [includeDeleted, key]);

    const handleDelete = async () => {
        if (!selected || deleting) return;
        setDeleting(true); setDeleteError('');
        try {
            await deleteCategory(selected.categoryId);
            setSelected(null); setMessage('카테고리가 삭제되었습니다.'); setReload(value => value + 1);
        } catch (failure) {
            setDeleteError(errorMessage(failure, '카테고리 삭제에 실패했습니다. 다시 시도해주세요.'));
        } finally { setDeleting(false); }
    };

    return <>
        <h1>카테고리 관리</h1>
        <div className="admin-toolbar">
            <button disabled={busy} onClick={() => { setMessage(''); setEditor({ category: null }); }}>카테고리 생성</button>
            <label><input type="checkbox" checked={includeDeleted} disabled={busy}
                onChange={event => { setMessage(''); setIncludeDeleted(event.target.checked); }} /> 삭제된 카테고리 보기</label>
        </div>
        {message && <p role="status">{message}</p>}
        {editor && <CategoryEditor key={editor.category?.categoryId ?? 'new'} category={editor.category}
            onSaved={() => { setMessage(editor.category ? '카테고리가 수정되었습니다.' : '카테고리가 생성되었습니다.'); setEditor(null); setReload(value => value + 1); }}
            onCancel={() => setEditor(null)} />}
        {loading ? <p>카테고리를 불러오는 중입니다.</p> : listing?.error
            ? <p role="alert">{listing.error} <button onClick={() => setReload(value => value + 1)}>다시 시도</button></p>
            : listing?.items.length === 0 ? <p>카테고리가 없습니다.</p>
                : <ul className="admin-category-list">{listing?.items.map(category => <li className="admin-category-row" key={category.categoryId}>
                    <span className="admin-category-name">{category.categoryName}{category.deleted && <span> (삭제됨)</span>}</span>
                    <div className="admin-category-actions">
                        <button disabled={busy} onClick={() => { setMessage(''); setEditor({ category }); }}>수정</button>
                        <button disabled={busy || category.deleted} onClick={() => {
                            setMessage(''); setDeleteError(''); setSelected(category);
                        }}>삭제</button>
                    </div>
                </li>)}</ul>}
        {selected && <DeleteCategoryDialog name={selected.categoryName} pending={deleting} error={deleteError}
            onConfirm={() => void handleDelete()} onCancel={() => setSelected(null)} />}
    </>;
}

export default AdminCategoryPage;
