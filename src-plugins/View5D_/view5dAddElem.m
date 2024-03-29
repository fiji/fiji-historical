function view5dAddElem(aviewer, in)
sz = imsize(in);
sz(6)=1;
sz(sz==0)=1;
if ~isreal(in)
  % Make a one dimensional flat input array
  inr = reshape(real(in),1,prod(sz));
  ini = reshape(imag(in),1,prod(sz));
  in = dip_array(reshape([inr ini],1,2*prod(sz)));
  aviewer.AddElementC(in,sz(1),sz(2),sz(3),1,sz(5));
else
  % Make a one dimensional flat input array
  in = dip_array(reshape(in,1,prod(sz)));
  aviewer.AddElement(in,sz(1),sz(2),sz(3),1,sz(5));
end
