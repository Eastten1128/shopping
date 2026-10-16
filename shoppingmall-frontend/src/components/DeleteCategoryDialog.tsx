import { useEffect, useId, useRef } from 'react';

interface Props {
    name: string;
    pending: boolean;
    error: string;
    onConfirm: () => void;
    onCancel: () => void;
}

function DeleteCategoryDialog({ name, pending, error, onConfirm, onCancel }: Props) {
    const titleId = useId();
    const dialog = useRef<HTMLDialogElement>(null);
    useEffect(() => {
        const element = dialog.current;
        element?.showModal();
        return () => element?.close();
    }, []);
    return <dialog ref={dialog} className="admin-delete-dialog" aria-labelledby={titleId} aria-busy={pending}
        onCancel={event => { event.preventDefault(); if (!pending) onCancel(); }}>
        <h2 id={titleId}>해당 카테고리를 삭제하시겠습니까?</h2>
        <p>{name}</p>
        {error && <p role="alert">{error}</p>}
        <button type="button" disabled={pending} onClick={onConfirm}>{pending ? '삭제 중...' : '예'}</button>{' '}
        <button type="button" disabled={pending} onClick={onCancel}>아니오</button>
    </dialog>;
}

export default DeleteCategoryDialog;
