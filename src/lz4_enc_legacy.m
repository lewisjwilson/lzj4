function lz4_encoder_final(fname, search_buffer)

%specify number of bytes to look back for a match
search_buffer = 6;

fname = '../test_files/swearing.txt';
extension = '.1';

tic
%Read file into an array
fid = fopen(fname, 'r');
data = fread(fid, 'uint8');
fclose(fid);

%fprintf("File Detected\nConverting to LZ4 file...\n")

%define a function for extra bytes needed for tokens
function [nibble plus_bytes] = hi_lo_plus(token)
  plus_bytes = [hex2dec(token)-15];
  total = plus_bytes(1);
  i=1;
  while total>=255
    plus_bytes(i)=255;
    total = total-255;
    plus_bytes(i+1)=total;
    i++;
  end
  nibble = dec2hex(15);
endfunction
  
matches=cell();

matches{1}=[];
last_byte_loc = [];


%finding the literal matches
for pos=1:length(data)
  %last_byte_location array population
  if length(last_byte_loc)==0
    last_byte_loc(end+1, 1)=data(pos);
    last_byte_loc(end, 2) = 0;
  else
    index = find(last_byte_loc(:,1)==data(pos));
    if index~=0
      last_byte_loc(end+1, 1)=data(pos);
      last_byte_loc(end, 2) = max(index);
    else
      last_byte_loc(end+1, 1)=data(pos);
      last_byte_loc(end, 2) = 0;
    end
  end
  
  [rows, ~] = size(last_byte_loc);
  matches{pos}=[];
  
  %where the current byte was last located  
  last_seen = last_byte_loc(rows, 2);
  
  %only searching where the byte was seen previously
  while last_seen>0
    %condition for limiting search range
    if last_seen>pos-search_buffer
      increase = 0;
      for cur = pos:length(data)
        if data(cur)~=data(last_seen+increase)
          break;
        end
        increase++;
      end
      if cur-pos>=4
        matches{pos} = [matches{pos} -(pos-last_seen) cur-pos];
      end
      last_seen = last_byte_loc(last_seen, 2);
    else
      break;
    end
  end
  
end


matches;

encoded_data = cell();
encoded_data = horzcat(encoded_data, [4 34 77 24 0 0 0 0 0 0 0]);
[~, cols] = size(matches);
literals = [];
pos=1;


while pos<=cols
  
 
  if length(matches{1,pos})!=0

    %check the maximum values in the current [matches] row
    [max_val idx] = max(matches{1, pos});
    max_ml_idx = idx(1);
    
    %length of literals
    hi_token = dec2hex(length(literals));
    %match length
    lo_token = dec2hex(matches{1, pos}(max_ml_idx)-4);
    
    hi_token_plus = '';
    if(hex2dec(hi_token)>=15)
      %run hi_lo_plus function
      [hi_token hi_token_plus] = hi_lo_plus(hi_token);
    else
      hi_token_plus = hex2dec(hi_token_plus);
    end
     
    lo_token_plus = '';
    if(hex2dec(lo_token)>=15)
      %run hi_lo_plus function
      [lo_token lo_token_plus] = hi_lo_plus(lo_token);
    else
      lo_token_plus = hex2dec(lo_token_plus);
    end
    
    %Evaluating the offset
    offset_byte1 = -matches{1, pos}(max_ml_idx-1);
    offset_byte2 = 0;
    %Little Endian
    if offset_byte1>=256
      offset_byte2 = floor(offset_byte1/256);
      while offset_byte1>=256
        offset_byte1 = offset_byte1-256;
      end
    end
   
    %concatenate hi and lo tokens
    token = hex2dec(strcat(hi_token, lo_token));
    
    %adding data to the encoded_data array
    if length(literals)==0
        encoded_data = horzcat(encoded_data, token, offset_byte1, 
        offset_byte2, lo_token_plus);
    else
        encoded_data = horzcat(encoded_data, token, hi_token_plus, 
        literals, offset_byte1, offset_byte2, lo_token_plus);
    end
    
    pos = pos+matches{1, pos}(max_ml_idx);
    literals = [];
    
  else
    literals = horzcat(literals, data(pos));
    pos++;
  end
end

%for extra literals at the end of the data
hi_token = dec2hex(length(literals));
hi_token_plus = '';
if(hex2dec(hi_token)>=15)
  %run hi_lo_plus function
  [hi_token hi_token_plus] = hi_lo_plus(hi_token); 
else
  hi_token_plus = hex2dec(hi_token_plus);
end

token = hex2dec(strcat(hi_token, '0'));
    
%Append ending marker
if length(literals)==0
  encoded_data = horzcat(encoded_data, [00 00 00 00]); 
else
  encoded_data = horzcat(encoded_data, token, hi_token_plus, 
  literals, 00, 00, [00 00 00 00]);
end
toc

%Output to file
fid = fopen(strcat(fname,'.lz4'), "w");
fprintf(fid, "%s", encoded_data{:});
fclose(fid);
fprintf("File Write Complete.\n")

fileinfo_before = stat(fname);
fsize_before = fileinfo_before.size;
fileinfo_after = stat(strcat(fname,'.lz4'));
fsize_after = fileinfo_after.size;

file_size_out = length(encoded_data);
compression = (fsize_after/fsize_before)*100;
fprintf("Original File Size: %d bytes\n", fsize_before)
fprintf("Encoded File Size: %d bytes\n", fsize_after)
fprintf("Compression: %d%%\n", compression)

endfunction