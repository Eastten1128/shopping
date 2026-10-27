import { useEffect, useId, useRef } from 'react';

interface Props {
    productName: string;
    pending: boolean;
    error: string;
    onConfirm: () => void;
    onCancel: () => void;
}

function StopProductDialog({ productName, pending, error, onConfirm, onCancel }: Props) {
    const titleId = useId();
    const dialog = useRef<HTMLDialogElement>(null);
    useEffect(() => {
        const element = dialog.current;
        element?.showModal();
        return () => element?.close();
    }, []);
    return (
        <dialog ref={dialog} aria-labelledby={titleId} aria-busy={pending} onCancel={event => {
            event.preventDefault();
            if (!pending) onCancel();
        }}>
            <h2 id={titleId}>상품 판매를 중지하겠습니까?</h2>
            <p>{productName}</p>
            {error && <p role="alert">{error}</p>}
            <button type="button" disabled={pending} onClick={onConfirm}>{pending ? '처리 중...' : '예'}</button>{' '}
            <button type="button" disabled={pending} onClick={onCancel}>아니오</button>
        </dialog>
    );
}

export default StopProductDialog;
